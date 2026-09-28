package com.backend.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.backend.backend.config.SafeScanProperties;
import com.backend.backend.exception.RateLimitExceededException;

import java.time.Duration;
import java.time.Instant;

/**
 * Fixed-window rate limiter backed by Redis: at most N scans per user per
 * minute. Checked before a credit is spent, so throttled requests are free.
 */
@Slf4j
@Service
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final int limitPerMinute;

    public RateLimitService(StringRedisTemplate redis, SafeScanProperties props) {
        this.redis = redis;
        this.limitPerMinute = props.rateLimit().scansPerMinute();
    }

    public void checkScanAllowed(long telegramUserId) {
        long minute = Instant.now().getEpochSecond() / 60;
        String key = "ratelimit:scan:" + telegramUserId + ":" + minute;
        Long count;
        try {
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(90));
            }
        } catch (RuntimeException e) {
            // Fail open: a Redis outage shouldn't take the whole bot down.
            // Credits still cap total usage per user.
            log.warn("Redis unavailable, skipping rate limit: {}", e.getMessage());
            return;
        }
        if (count != null && count > limitPerMinute) {
            throw new RateLimitExceededException();
        }
    }
}

