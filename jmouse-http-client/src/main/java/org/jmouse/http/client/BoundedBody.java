package org.jmouse.http.client;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * An answer's body read to its end, or refused — the connection cancelled — once it passes its limit: at once where the
 * answer declares a longer length, else on the batch that would cross it, which is never kept.
 */
final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {

    private final CompletableFuture<byte[]> completion = new CompletableFuture<>();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final long limit;
    private final OptionalLong declared;
    private Flow.Subscription subscription;

    /** @param declared the length the answer says its body is, where it says one */
    BoundedBody(long limit, OptionalLong declared) {
        this.limit = limit;
        this.declared = declared;
    }

    /** The body of an answer as its headers describe it. */
    static BoundedBody of(long limit, HttpResponse.ResponseInfo information) {
        return new BoundedBody(limit, information.headers().firstValueAsLong("Content-Length"));
    }

    @Override
    public CompletionStage<byte[]> getBody() {
        return completion;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        if (declared.isPresent() && declared.getAsLong() > limit) {
            refuse();
            return;
        }
        subscription.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        if (completion.isDone()) {
            return;
        }
        long arriving = buffers.stream().mapToLong(ByteBuffer::remaining).sum();
        if (bytes.size() + arriving > limit) {
            refuse();
            return;
        }
        for (ByteBuffer buffer : buffers) {
            byte[] part = new byte[buffer.remaining()];
            buffer.get(part);
            bytes.writeBytes(part);
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable failure) {
        completion.completeExceptionally(failure);
    }

    @Override
    public void onComplete() {
        completion.complete(bytes.toByteArray());
    }

    private void refuse() {
        subscription.cancel();
        completion.completeExceptionally(new HttpCallException(HttpCallException.Failure.RESPONSE_LIMIT,
                "The answer is longer than the " + limit + " bytes the call reads."));
    }
}
