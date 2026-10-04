package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Entire response completion is timed and bounded before JSON parsing. HTTP redirects are disabled. */
public final class JdkEmbeddingTransport implements EmbeddingTransport, AutoCloseable {

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client;

    public JdkEmbeddingTransport(Duration connectTimeout) {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("Positive transport connect timeout is required.");
        }
        client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public Object post(URI endpoint, ProviderSettings settings, Object body, CallLimits limits) {
        return post(endpoint, settings, body, EmbeddingTransport.bearerHeaders(settings), limits);
    }

    @Override
    public Object post(URI endpoint, ProviderSettings settings, Object body, Map<String, String> headers, CallLimits limits) {
        endpoint = EmbeddingTransport.requireEndpoint(endpoint.toString());
        HttpRequest request = request(endpoint, body, headers, limits);
        var pending = client.sendAsync(request, information -> new BoundedResponseBody(limits.maximumResponseBytes()));
        return awaitResponse(pending, limits);
    }

    private HttpRequest request(URI endpoint, Object body, Map<String, String> headers, CallLimits limits) {
        try {
            byte[] encoded = json.writeValueAsBytes(body);
            var builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofMillis(limits.timeoutMilliseconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(encoded));
            headers.forEach(builder::header);
            return builder.build();
        } catch (IOException | IllegalArgumentException failure) {
            // Header/serialization errors must not expose credentials or caller input in diagnostics.
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
    }

    private Object awaitResponse(CompletableFuture<HttpResponse<byte[]>> pending, CallLimits limits) {
        try {
            var response = pending.get(limits.timeoutMilliseconds(), TimeUnit.MILLISECONDS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EmbeddingException(EmbeddingException.Reason.PROVIDER_REFUSED, response.statusCode());
            }
            return json.readValue(response.body(), Object.class);
        } catch (TimeoutException failure) {
            pending.cancel(true);
            throw new EmbeddingException(EmbeddingException.Reason.TIMEOUT, null);
        } catch (InterruptedException failure) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new EmbeddingException(EmbeddingException.Reason.TRANSPORT, null);
        } catch (ExecutionException failure) {
            pending.cancel(true);
            if (failure.getCause() instanceof EmbeddingException known) {
                throw known;
            }
            if (failure.getCause() instanceof HttpTimeoutException) {
                throw new EmbeddingException(EmbeddingException.Reason.TIMEOUT, null);
            }
            throw new EmbeddingException(EmbeddingException.Reason.TRANSPORT, null);
        } catch (IOException failure) {
            throw new EmbeddingException(EmbeddingException.Reason.INVALID_RESPONSE, null);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private static final class BoundedResponseBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> completion = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final int maximum;
        private Flow.Subscription subscription;

        private BoundedResponseBody(int maximum) {
            this.maximum = maximum;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return completion;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if ((long) bytes.size() + buffer.remaining() > maximum) {
                    subscription.cancel();
                    completion.completeExceptionally(new EmbeddingException(EmbeddingException.Reason.RESPONSE_LIMIT, null));
                    return;
                }
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
    }
}
