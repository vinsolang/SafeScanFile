package com.backend.backend.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Typed view of the {@code safescan.*} block in application.yml. */
@ConfigurationProperties (prefix = "safescan")
public record SafeScanProperties (
    @DefaultValue Telegram telegram,
    @DefaultValue Scanner scanner,
    @DefaultValue RateLimit rateLimit
){

    public record Telegram(
        @DefaultValue ("") String botToken,
        @DefaultValue ("") String botUserName
    ) {

    }

    public record Scanner(
        @DefaultValue("localhost") String clamavHost,
        @DefaultValue("3310") int clamavPort,
        @DefaultValue("/t.me/ScanSafeBot") String tmpDir,
        // Telegram's Bot API only lets bots download files up to 20 MB.
        @DefaultValue("20") int maxFileSizeMb,
        @DefaultValue("60000") int timeoutMs
    ) {
        public long maxFileSizeBytes(){
            return maxFileSizeMb * 1024L * 1024L;
        }
    }

    public record RateLimit(@DefaultValue ("10") int scansPerMinute) {

    }
}
