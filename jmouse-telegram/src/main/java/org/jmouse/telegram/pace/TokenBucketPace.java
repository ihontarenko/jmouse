package org.jmouse.telegram.pace;

import org.jmouse.core.throttle.RateLimiter;
import org.jmouse.telegram.ChatReference;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A global bucket, a bucket per chat, and a penalty clock — in this process.
 *
 * <p>Built over {@code jmouse-core}'s {@link RateLimiter} rather than reimplementing token-bucket
 * arithmetic, exactly as {@code TokenBucketCallerRateLimiter} is. What this adds is the waiting, the
 * per-chat key, and Telegram's own penalty overriding both.
 *
 * <p>⚠️ <strong>In this process.</strong> Two instances of an application share a bot but not these
 * buckets, so the effective rate is doubled and Telegram counts the total. A deployment that runs more
 * than one instance wants a {@link Pace} over a shared cache; this class is the single-instance
 * default, not the general answer.
 *
 * <p>⚠️ <strong>The chat map is bounded</strong>, because a chat id arrives from outside and a map
 * keyed by one grows for as long as ids keep being new. Past {@link #DEFAULT_TRACKED_CHATS} it is
 * cleared wholesale rather than evicted one at a time: a chat whose bucket is discarded gets a fresh
 * full one, which is the permissive direction — and the global bucket, which is not keyed by anything,
 * still holds the line.
 */
public final class TokenBucketPace implements Pace {

    /** Telegram's published overall guidance: about thirty messages a second. */
    public static final int DEFAULT_GLOBAL_PER_SECOND = 30;

    /** Telegram's published per-group guidance: about twenty messages a minute. */
    public static final int DEFAULT_PER_CHAT_PER_MINUTE = 20;

    /** Upper bound on tracked chats, so the bucket map cannot itself become the problem. */
    public static final int DEFAULT_TRACKED_CHATS = 10_000;

    /** How often a waiting thread re-asks. Short enough not to add visible latency. */
    private static final Duration POLL_INTERVAL = Duration.ofMillis(50);

    private final Map<String, RateLimiter> chatBuckets = new ConcurrentHashMap<>();

    /** Epoch millis before which nothing may be sent at all, set by {@link #penalise}. */
    private final AtomicLong globalPenaltyUntil = new AtomicLong();

    private final Map<String, Long> chatPenaltyUntil = new ConcurrentHashMap<>();

    private final RateLimiter globalBucket;
    private final int         perChatPerMinute;
    private final int         trackedChats;

    public TokenBucketPace(int globalPerSecond, int perChatPerMinute, int trackedChats) {
        this.globalBucket     = RateLimiter.smooth(globalPerSecond, Duration.ofSeconds(1).toNanos());
        this.perChatPerMinute = perChatPerMinute;
        this.trackedChats     = trackedChats;
    }

    /** Telegram's published guidance, which is the right starting point and not a guarantee. */
    public static TokenBucketPace defaults() {
        return new TokenBucketPace(
                DEFAULT_GLOBAL_PER_SECOND, DEFAULT_PER_CHAT_PER_MINUTE, DEFAULT_TRACKED_CHATS);
    }

    @Override
    public void awaitTurn(ChatReference chat) {
        String key = chat.wireValue();

        awaitPenalty(globalPenaltyUntil.get());
        awaitPenalty(chatPenaltyUntil.getOrDefault(key, 0L));

        // ⚠️ The global permit is taken FIRST and the per-chat one second. The other order lets a
        // thread hold a scarce per-chat permit while queueing for the global bucket, which is the
        // shape that starves other chats.
        awaitPermit(globalBucket);
        awaitPermit(bucketFor(key));
    }

    @Override
    public void penalise(ChatReference chat, Duration retryAfter) {
        long until = System.currentTimeMillis() + retryAfter.toMillis();

        chatPenaltyUntil.put(chat.wireValue(), until);

        // A flood wait is usually about one chat, but Telegram also applies a global one - and it does
        // not say which. Raising both is the safe reading; the cost of being wrong is a short pause.
        globalPenaltyUntil.accumulateAndGet(until, Math::max);
    }

    private RateLimiter bucketFor(String key) {
        if (chatBuckets.size() >= trackedChats) {
            chatBuckets.clear();
        }

        return chatBuckets.computeIfAbsent(key,
                ignored -> RateLimiter.smooth(perChatPerMinute, Duration.ofMinutes(1).toNanos()));
    }

    private void awaitPermit(RateLimiter bucket) {
        while (!bucket.tryAcquire()) {
            sleep(POLL_INTERVAL.toMillis());
        }
    }

    private void awaitPenalty(long until) {
        long remaining = until - System.currentTimeMillis();

        if (remaining > 0) {
            sleep(remaining);
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            // ⚠️ Restore the flag and stop. An outbox worker or a polling loop is shut down by
            // interrupting it, and a pace that kept waiting would be a thread that never exits.
            Thread.currentThread().interrupt();
            throw new CancellationException("interrupted while waiting for Telegram's pace");
        }
    }
}
