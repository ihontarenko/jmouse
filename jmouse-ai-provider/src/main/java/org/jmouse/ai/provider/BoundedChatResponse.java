package org.jmouse.ai.provider;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** JDK body contract: cancel oversized declared or streamed bodies before forwarding bytes to the accumulator. */
final class BoundedChatResponse implements HttpResponse.BodyHandler<String> {

    private final int maximumResponseBytes;

    BoundedChatResponse(int maximumResponseBytes) {
        this.maximumResponseBytes = maximumResponseBytes;
    }

    @Override
    public HttpResponse.BodySubscriber<String> apply(HttpResponse.ResponseInfo information) {
        long declaredBytes = information.headers().firstValueAsLong("Content-Length").orElse(-1);
        return new LimitedBody(maximumResponseBytes, declaredBytes > maximumResponseBytes);
    }

    private static final class LimitedBody implements HttpResponse.BodySubscriber<String> {

        private final HttpResponse.BodySubscriber<String> delegate = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        private final int maximumResponseBytes;
        private final boolean declaredOversized;
        private Flow.Subscription subscription;
        private long receivedBytes;
        private boolean finished;

        private LimitedBody(int maximumResponseBytes, boolean declaredOversized) {
            this.maximumResponseBytes = maximumResponseBytes;
            this.declaredOversized = declaredOversized;
        }

        @Override
        public CompletionStage<String> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription incoming) {
            if (subscription != null) {
                incoming.cancel();
                return;
            }
            subscription = incoming;
            if (declaredOversized) {
                // Initialize the delegate without asking the HTTP publisher for any body bytes.
                delegate.onSubscribe(new Flow.Subscription() {
                    public void request(long count) { }
                    public void cancel() { }
                });
                refuse();
            } else {
                delegate.onSubscribe(incoming);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (finished) {
                return;
            }
            long batchBytes = buffers.stream().mapToLong(ByteBuffer::remaining).sum();
            if (batchBytes > maximumResponseBytes - receivedBytes) {
                refuse();
                return;
            }
            receivedBytes += batchBytes;
            delegate.onNext(buffers);
        }

        @Override
        public void onError(Throwable failure) {
            if (!finished) {
                finished = true;
                delegate.onError(failure);
            }
        }

        @Override
        public void onComplete() {
            if (!finished) {
                finished = true;
                delegate.onComplete();
            }
        }

        private void refuse() {
            finished = true;
            subscription.cancel();
            delegate.onError(new ChatResponseLimitException(maximumResponseBytes));
        }
    }
}
