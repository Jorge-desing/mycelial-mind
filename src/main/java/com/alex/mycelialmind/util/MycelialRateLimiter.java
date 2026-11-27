package com.alex.mycelialmind.util;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LocalBucketBuilder;

import java.time.Duration;

/**
 * Token-bucket burst rate limiter backed by Bucket4j.
 * Supports deterministic testing via custom {@link TimeMeter} instances.
 */
public final class MycelialRateLimiter {
    private final Bucket bucket;

    public MycelialRateLimiter(long capacity, long refillTokens, Duration refillPeriod) {
        this(capacity, refillTokens, refillPeriod, null);
    }

    public MycelialRateLimiter(long capacity, long refillTokens, Duration refillPeriod, TimeMeter timeMeter) {
        long safeCapacity = Math.max(1L, capacity);
        long safeRefill = Math.max(1L, refillTokens);
        Duration safePeriod = (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative())
                ? Duration.ofSeconds(1)
                : refillPeriod;
        LocalBucketBuilder builder = Bucket.builder()
                .addLimit(limit -> limit.capacity(safeCapacity).refillGreedy(safeRefill, safePeriod));
        if (timeMeter != null) {
            builder.withCustomTimePrecision(timeMeter);
        }
        this.bucket = builder.build();
    }

    public boolean tryConsume() {
        return bucket.tryConsume(1L);
    }

    public boolean tryConsume(long tokens) {
        return tokens <= 0L || bucket.tryConsume(tokens);
    }

    public long getAvailableTokens() {
        return bucket.getAvailableTokens();
    }

    public void reset() {
        bucket.reset();
    }
}
