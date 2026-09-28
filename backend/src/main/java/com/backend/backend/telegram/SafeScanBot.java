package com.backend.backend.telegram;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.backend.backend.dto.IncomingFile;
import com.backend.backend.dto.ScanReport;
import com.backend.backend.exception.FileTooLargeException;
import com.backend.backend.exception.InsufficientCreditsException;
import com.backend.backend.exception.InvalidFileException;
import com.backend.backend.exception.RateLimitExceededException;
import com.backend.backend.exception.SafeScanException;
import com.backend.backend.exception.UserBlockedException;
import com.backend.backend.models.User;
import com.backend.backend.models.UserStatus;
import com.backend.backend.services.PlanService;
import com.backend.backend.services.ScanWorkflowService;
import com.backend.backend.services.UserService;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Telegram front-end: routes commands and uploaded documents to the
 * services. Holds no business logic of its own.
 *
 * Each update is handled on its own virtual thread so one slow scan never
 * blocks other users' messages; a semaphore caps how many scans (and thus
 * downloads / clamd connections) run at once.
 */
@Slf4j
public class SafeScanBot implements LongPollingSingleThreadUpdateConsumer {

    private static final int MAX_CONCURRENT_SCANS = 8;
    private static final int HISTORY_LIMIT = 10;

    private final TelegramClient client;
    private final UserService users;
    private final ScanWorkflowService workflow;
    private final PlanService plans;
    private final long maxFileMb;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore scanSlots = new Semaphore(MAX_CONCURRENT_SCANS);

    public SafeScanBot(TelegramClient client, UserService users, ScanWorkflowService workflow,
                       PlanService plans, long maxFileMb) {
        this.client = client;
        this.users = users;
        this.workflow = workflow;
        this.plans = plans;
        this.maxFileMb = maxFileMb;
    }

    @Override
    public void consume(Update update) {
        executor.execute(() -> {
            try {
                handle(update);
            } catch (Exception e) {
                log.error("Unhandled error while handling update {}", update.getUpdateId(), e);
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private void handle(Update update) {
        Message message = update.getMessage();
        if (message == null || message.getFrom() == null
                || Boolean.TRUE.equals(message.getFrom().getIsBot())) {
            return;
        }
        // Private chats only: credits belong to a person, not a group.
        if (!"private".equals(message.getChat().getType())) {
            return;
        }
        long chatId = message.getChatId();
        var from = message.getFrom();
        User user = users.registerOrUpdate(from.getId(), from.getUserName(), from.getFirstName());

        if (user.getStatus() == UserStatus.BLOCKED) {
            send(chatId, BotMessages.blocked());
        } else if (message.hasDocument()) {
            handleDocument(chatId, user, message.getDocument());
        } else if (message.hasText()) {
            handleText(chatId, user, message.getText());
        } else {
            send(chatId, BotMessages.sendAsDocument());
        }
    }

    private void handleText(long chatId, User user, String text) {
        String command = text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int at = command.indexOf('@'); // "/start@SafeScanBot" in some clients
        if (at > 0) {
            command = command.substring(0, at);
        }
        switch (command) {
            case "/start" -> send(chatId, BotMessages.welcome(user.getFirstName(), workflow.balance(user.getId())));
            case "/help" -> send(chatId, BotMessages.help());
            case "/scan" -> send(chatId, BotMessages.scanPrompt(maxFileMb));
            case "/balance" -> send(chatId, BotMessages.balance(workflow.balance(user.getId())));
            case "/plans" -> send(chatId, BotMessages.plans(plans.activePlans()));
            case "/buy" -> send(chatId, BotMessages.paymentsSoon());
            case "/history" -> send(chatId, BotMessages.history(workflow.recentScans(user.getId(), HISTORY_LIMIT)));
            default -> send(chatId, BotMessages.unknownCommand());
        }
    }

    private void handleDocument(long chatId, User user, Document doc) {
        // Number so this compiles whether the library exposes Integer or Long.
        Number declared = doc.getFileSize();
        IncomingFile file = new IncomingFile(
                doc.getFileName(),
                declared == null ? null : declared.longValue(),
                doc.getMimeType(),
                () -> download(doc.getFileId()));

        try {
            scanSlots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            send(chatId, BotMessages.checking());
            ScanReport report = workflow.scanFile(user, file);
            send(chatId, BotMessages.scanReport(report));
        } catch (InsufficientCreditsException e) {
            send(chatId, BotMessages.noCredits());
        } catch (RateLimitExceededException e) {
            send(chatId, BotMessages.rateLimited());
        } catch (FileTooLargeException e) {
            send(chatId, BotMessages.tooLarge(e.getMaxBytes()));
        } catch (InvalidFileException e) {
            send(chatId, e.getMessage());
        } catch (UserBlockedException e) {
            send(chatId, BotMessages.blocked());
        } catch (SafeScanException e) {
            log.warn("Scan failed for user {}: {}", user.getId(), e.getMessage(), e);
            send(chatId, BotMessages.scanFailed());
        } finally {
            scanSlots.release();
        }
    }

    /** Two-step Telegram download: resolve the file path, then stream the bytes. */
    private java.io.InputStream download(String fileId) throws IOException {
        try {
            var telegramFile = client.execute(GetFile.builder().fileId(fileId).build());
            return client.downloadFileAsStream(telegramFile);
        } catch (TelegramApiException e) {
            throw new IOException("Telegram download failed", e);
        }
    }

    private void send(long chatId, String text) {
        try {
            client.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            log.warn("Could not send message to chat {}: {}", chatId, e.getMessage());
        }
    }
}

