package com.backend.backend.telegram;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.backend.backend.dto.ScanReport;
import com.backend.backend.models.Plan;
import com.backend.backend.models.Scan;
import com.backend.backend.models.ScanStatus;

/**
 * All user-facing text. Plain text only (no Markdown/HTML parse mode) on
 * purpose: file names and threat names are untrusted input, and plain text
 * means they can never inject formatting or links into a reply.
 *
 * Wording says "No malware detected", never "safe": no scanner is perfect.
 */
final class BotMessages {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BotMessages() {
    }

    static String welcome(String firstName, long credits) {
        String name = (firstName == null || firstName.isBlank()) ? "there" : firstName;
        return "👋 Welcome to SafeScan, " + name + "!\n\n"
                + "🛡️ Scan files for malware and suspicious content.\n\n"
                + "Remaining scans: " + credits + "\n\n"
                + "Just send me a file (as a document) and I'll scan it.\n\n"
                + commandList();
    }

    static String help() {
        return "🛡️ SafeScan checks files for malware.\n\n"
                + "Send any file as a document and I'll scan it with ClamAV. "
                + "Files are deleted right after the scan, never opened or run.\n\n"
                + "Note: \"no malware detected\" is not a guarantee. "
                + "Be careful with files from sources you don't trust.\n\n"
                + commandList();
    }

    private static String commandList() {
        return "Commands:\n"
                + "/scan - How to scan a file\n"
                + "/balance - Check remaining scans\n"
                + "/plans - View plans\n"
                + "/buy - Buy a plan\n"
                + "/history - Scan history\n"
                + "/help - Help";
    }

    static String scanPrompt(long maxMb) {
        return "📎 Send me the file you want to scan, as a document (attach → File).\n"
                + "Maximum size: " + maxMb + " MB.";
    }

    static String balance(long credits) {
        return "Remaining scans: " + credits;
    }

    static String checking() {
        return "🔍 Checking your file…";
    }

    static String scanReport(ScanReport r) {
        return switch (r.status()) {
            case CLEAN -> "✅ Scan completed\n\n"
                    + "File:\n" + r.fileName() + " (" + humanSize(r.fileSize()) + ")\n\n"
                    + "Result:\n🟢 No malware detected\n\n"
                    + "SHA-256:\n" + r.sha256() + "\n\n"
                    + "No scanner catches everything, so stay cautious with files you don't trust.\n\n"
                    + "Remaining scans: " + r.remainingCredits();
            case THREAT_FOUND -> "🚨 Threat detected\n\n"
                    + "File:\n" + r.fileName() + " (" + humanSize(r.fileSize()) + ")\n\n"
                    + "Result:\n🔴 " + r.threatName() + "\n\n"
                    + "SHA-256:\n" + r.sha256() + "\n\n"
                    + "The file was not kept on our server. "
                    + "Please do not open or share it.\n\n"
                    + "Remaining scans: " + r.remainingCredits();
            default -> scanFailed();
        };
    }

    static String scanFailed() {
        return "⚠️ The scan could not be completed.\n\n"
                + "Your scan credit was not used. Please try again in a few minutes.";
    }

    static String noCredits() {
        return "You have no scans left.\n\nUse /plans to see what's available.";
    }

    static String rateLimited() {
        return "⏳ You're sending files too fast. Please wait a minute and try again.";
    }

    static String tooLarge(long maxBytes) {
        return "That file is too large. The maximum size is " + humanSize(maxBytes) + ".";
    }

    static String blocked() {
        return "This account has been blocked.";
    }

    static String sendAsDocument() {
        return "Please send the file as a document (attach → File), "
                + "not as a photo or video, so it isn't compressed.";
    }

    static String unknownCommand() {
        return "I didn't understand that. Send me a file to scan, or use /help.";
    }

    static String paymentsSoon() {
        return "💳 Payments aren't available yet. Use /plans to see the upcoming plans.";
    }

    static String plans(List<Plan> plans) {
        if (plans.isEmpty()) {
            return "No plans are available right now.";
        }
        StringBuilder sb = new StringBuilder("💰 Plans\n");
        for (Plan p : plans) {
            String price = p.getPrice().signum() == 0
                    ? "Free"
                    : "$" + p.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
            sb.append("\n").append(p.getName()).append(" — ").append(price)
                    .append(" — ").append(p.getScanLimit()).append(" scans");
        }
        return sb.toString();
    }

    static String history(List<Scan> scans) {
        if (scans.isEmpty()) {
            return "No scans yet. Send me a file to get started.";
        }
        StringBuilder sb = new StringBuilder("🧾 Your last " + scans.size() + " scans\n");
        for (Scan s : scans) {
            sb.append("\n").append(emoji(s.getScanStatus())).append(" ").append(s.getFileName());
            if (s.getScanStatus() == ScanStatus.THREAT_FOUND && s.getThreatName() != null) {
                sb.append(" — ").append(s.getThreatName());
            }
            sb.append("\n    ").append(s.getCreatedAt().format(TIME));
        }
        return sb.toString();
    }

    private static String emoji(ScanStatus status) {
        return switch (status) {
            case CLEAN -> "🟢";
            case THREAT_FOUND -> "🔴";
            case ERROR -> "⚠️";
            default -> "⏳";
        };
    }

    static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        return String.format(Locale.ROOT, "%.1f MB", kb / 1024.0);
    }
}

