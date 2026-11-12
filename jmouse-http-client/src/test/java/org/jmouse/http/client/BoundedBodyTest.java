package org.jmouse.http.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("an answer's body read within its limit")
class BoundedBodyTest {

    private static Flow.Subscription subscription(AtomicBoolean cancelled) {
        return new Flow.Subscription() {
            @Override
            public void request(long count) {
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }
        };
    }

    private static HttpCallException refusal(BoundedBody body) {
        CompletionException failure = assertThrows(CompletionException.class, () -> body.getBody().toCompletableFuture().join());
        return assertInstanceOf(HttpCallException.class, failure.getCause());
    }

    @Test
    @DisplayName("a body exactly as long as the limit is read whole, a character split between batches included")
    void theExactLimitIsRead() {
        byte[] bytes = "Київ😀".getBytes(StandardCharsets.UTF_8);
        AtomicBoolean cancelled = new AtomicBoolean();
        BoundedBody body = new BoundedBody(bytes.length, OptionalLong.empty());
        body.onSubscribe(subscription(cancelled));
        body.onNext(List.of(ByteBuffer.wrap(bytes, 0, 3)));
        body.onNext(List.of(ByteBuffer.wrap(bytes, 3, bytes.length - 3)));
        body.onComplete();
        assertArrayEquals(bytes, body.getBody().toCompletableFuture().join());
        assertFalse(cancelled.get());
    }

    @Test
    @DisplayName("the batch that would cross the limit cancels the answer and is not kept")
    void aBatchPastTheLimitCancels() {
        AtomicBoolean cancelled = new AtomicBoolean();
        BoundedBody body = new BoundedBody(4, OptionalLong.empty());
        body.onSubscribe(subscription(cancelled));
        body.onNext(List.of(ByteBuffer.wrap(new byte[4])));
        body.onNext(List.of(ByteBuffer.wrap(new byte[1])));
        body.onComplete();
        assertEquals(HttpCallException.Failure.RESPONSE_LIMIT, refusal(body).failure());
        assertTrue(cancelled.get());
    }

    @Test
    @DisplayName("an answer declaring a longer body is refused before a byte of it is read")
    void aDeclaredLengthPastTheLimitIsRefused() {
        AtomicBoolean cancelled = new AtomicBoolean();
        BoundedBody body = new BoundedBody(4, OptionalLong.of(5));
        body.onSubscribe(subscription(cancelled));
        assertEquals(HttpCallException.Failure.RESPONSE_LIMIT, refusal(body).failure());
        assertTrue(cancelled.get());
    }
}
