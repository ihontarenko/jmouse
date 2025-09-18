package org.jmouse.core.limits;

import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketV2 {

    private final long          capacity;
    private final double        refillPerNanos; // tokens per nanosecond
    private       double        tokens;
    private       long          lastRefillNanos;
    private final ReentrantLock lock = new ReentrantLock();

    TokenBucketV2(long capacity, long periodNanos) {
        this.capacity = capacity;
        this.refillPerNanos = (double) capacity / (double) periodNanos;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    boolean tryConsume() {
        long now = System.nanoTime();
        lock.lock();
        try {
            refill(now);
            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void refill(long now) {
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) return;
        tokens = Math.min(capacity, tokens + elapsed * refillPerNanos);
        lastRefillNanos = now;
    }

}
