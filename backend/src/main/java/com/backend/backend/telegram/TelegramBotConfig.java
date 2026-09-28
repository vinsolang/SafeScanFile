package com.backend.backend.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.backend.backend.config.SafeScanProperties;
import com.backend.backend.services.PlanService;
import com.backend.backend.services.ScanWorkflowService;
import com.backend.backend.services.UserService;

/**
 * Wires the bot and starts long polling. Only active when
 * TELEGRAM_BOT_TOKEN is set, so the rest of the app (DB, scanner, health
 * endpoint) still runs and can be tested without a bot.
 */
@Slf4j
@Configuration
public class TelegramBotConfig {

    private static final String TOKEN_SET = "'${safescan.telegram.bot-token:}' != ''";
    private static final String TOKEN_MISSING = "'${safescan.telegram.bot-token:}' == ''";

    @Configuration
    @ConditionalOnExpression(TOKEN_SET)
    static class Enabled {

        @Bean
        TelegramClient telegramClient(SafeScanProperties props) {
            return new OkHttpTelegramClient(props.telegram().botToken());
        }

        @Bean
        SafeScanBot safeScanBot(TelegramClient client, UserService users,
                                ScanWorkflowService workflow, PlanService plans,
                                SafeScanProperties props) {
            return new SafeScanBot(client, users, workflow, plans, props.scanner().maxFileSizeMb());
        }

        /** Starts polling on creation; closed automatically on shutdown. */
        @Bean(destroyMethod = "close")
        TelegramBotsLongPollingApplication botsApplication(SafeScanProperties props, SafeScanBot bot)
                throws TelegramApiException {
            TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication();
            app.registerBot(props.telegram().botToken(), bot);
            log.info("Telegram bot started (long polling)");
            return app;
        }
    }

    @Configuration
    @ConditionalOnExpression(TOKEN_MISSING)
    static class Disabled {

        @Bean
        ApplicationRunner telegramDisabledNotice() {
            return args -> log.warn(
                    "TELEGRAM_BOT_TOKEN is not set - the Telegram bot is DISABLED. "
                            + "Create a bot with @BotFather and set the token to enable it.");
        }
    }
}

