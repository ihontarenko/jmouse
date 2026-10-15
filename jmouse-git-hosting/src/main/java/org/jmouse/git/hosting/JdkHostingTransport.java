package org.jmouse.git.hosting;

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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link HostingTransport} over the JDK {@link HttpClient}. The whole response is timed and a success body is capped
 * in bytes while it streams, before any JSON is parsed; an expired call is cancelled. A refusal's body is discarded
 * unread. Redirects are never followed, so credentials cannot be carried elsewhere. The client is reusable; the
 * adopter closes it.
 */
public final class JdkHostingTransport implements HostingTransport, AutoCloseable {

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient   client;

    public JdkHostingTransport(Duration connectTimeout) {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("Positive transport connect timeout is required.");
        }
        client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public HostingHttpResponse get(URI endpoint, Map<String, String> headers, HostingLimits limits) {
        HttpRequest request = request(requireEndpoint(endpoint), headers, limits);
        CompletableFuture<HttpResponse<byte[]>> pending = client.sendAsync(request, information -> {
            if (information.statusCode() >= 200 && information.statusCode() < 300) {
                return new BoundedResponseBody(limits.maximumResponseBytes());
            }
            return HttpResponse.BodySubscribers.<byte[]>replacing(null);
        });
        return awaitResponse(pending, limits);
    }

    private static URI requireEndpoint(URI endpoint) {
        if (endpoint == null || !Set.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawFragment() != null) {
            throw new HostingException(HostingException.Reason.CONFIGURATION);
        }
        return endpoint;
    }

    private static HttpRequest request(URI endpoint, Map<String, String> headers, HostingLimits limits) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofMillis(limits.timeoutMilliseconds()))
                    .GET();
            headers.forEach(builder::header);
            return builder.build();
        } catch (IllegalArgumentException failure) {
            // A rejected header value may be a credential; the diagnostic must not repeat it.
            throw new HostingException(HostingException.Reason.CONFIGURATION);
        }
    }

    private HostingHttpResponse awaitResponse(CompletableFuture<HttpResponse<byte[]>> pending, HostingLimits limits) {
        try {
            HttpResponse<byte[]> response = pending.get(limits.timeoutMilliseconds(), TimeUnit.MILLISECONDS);
            Object body = null;
            if (response.body() != null && response.body().length > 0) {
                body = json.readValue(response.body(), Object.class);
            }
            return new HostingHttpResponse(response.statusCode(), response.headers(), body);
        } catch (TimeoutException failure) {
            pending.cancel(true);
            throw new HostingException(HostingException.Reason.TIMEOUT);
        } catch (InterruptedException failure) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new HostingException(HostingException.Reason.TRANSPORT);
        } catch (ExecutionException failure) {
            pending.cancel(true);
            if (failure.getCause() instanceof HostingException known) {
                throw known;
            }
            if (failure.getCause() instanceof HttpTimeoutException) {
                throw new HostingException(HostingException.Reason.TIMEOUT);
            }
            throw new HostingException(HostingException.Reason.TRANSPORT);
        } catch (IOException failure) {
            throw new HostingException(HostingException.Reason.INVALID_RESPONSE);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private static final class BoundedResponseBody implements HttpResponse.BodySubscriber<byte[]> {

        private final CompletableFuture<byte[]> completion = new CompletableFuture<>();
        private final ByteArrayOutputStream     bytes      = new ByteArrayOutputStream();
        private final int                       maximum;
        private Flow.Subscription               subscription;

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
                    completion.completeExceptionally(new HostingException(HostingException.Reason.RESPONSE_LIMIT));
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
