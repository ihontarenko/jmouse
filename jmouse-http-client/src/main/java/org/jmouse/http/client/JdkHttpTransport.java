package org.jmouse.http.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Calls made by the JDK's own client. The whole call is timed — the answer's body read to its end included, which the
 * JDK's own request timeout does not cover — and the body is read only up to the call's limit, the connection cancelled
 * past it. Redirects are followed only where the transport was made to.
 */
public final class JdkHttpTransport implements HttpTransport, AutoCloseable {

    /** Whether a transport follows a redirect. */
    public enum Redirects {
        /** Never: a redirect is an answer like any other, its caller's to follow. */
        NEVER,
        /** As a browser does, never from HTTPS to HTTP. */
        NORMAL
    }

    /**
     * The headers the JDK's client writes itself and refuses from a caller: a call naming one has it written by the client.
     * Leaving the caller's value out is right — a run's standing headers are written once for every site, and one of them
     * being a name the client manages must not fail every call.
     */
    private static final Set<String> MANAGED = Set.of("connection", "content-length", "expect", "host", "upgrade");

    private final HttpClient client;

    /** @param connectTimeout how long making a connection may take */
    public JdkHttpTransport(Duration connectTimeout, Redirects redirects) {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("A transport connects in a positive time, not " + connectTimeout + ".");
        }
        client = HttpClient.newBuilder().connectTimeout(connectTimeout)
                .followRedirects(redirects == Redirects.NORMAL ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER).build();
    }

    @Override
    public HttpReply exchange(HttpCall call) {
        CompletableFuture<HttpResponse<byte[]>> pending = client.sendAsync(request(call),
                information -> BoundedBody.of(call.responseLimit(), information));
        try {
            HttpResponse<byte[]> response = pending.get(call.timeout().toMillis(), TimeUnit.MILLISECONDS);
            return new HttpReply(response.uri(), response.statusCode(), response.headers().map(), response.body());
        } catch (TimeoutException late) {
            pending.cancel(true);
            throw new HttpCallException(HttpCallException.Failure.TIMEOUT, "No answer from " + call.uri().getHost()
                    + " within " + call.timeout().toMillis() + " ms.");
        } catch (InterruptedException interrupted) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new HttpCallException(HttpCallException.Failure.INTERRUPTED, "Waiting for " + call.uri().getHost()
                    + " was interrupted.");
        } catch (ExecutionException failed) {
            pending.cancel(true);
            throw failureOf(call, failed.getCause());
        }
    }

    private static HttpRequest request(HttpCall call) {
        HttpRequest.Builder request = HttpRequest.newBuilder(call.uri()).timeout(call.timeout());
        call.headers().forEach((name, values) -> {
            if (!MANAGED.contains(name.toLowerCase(Locale.ROOT))) {
                values.forEach(value -> request.header(name, value));
            }
        });
        HttpRequest.BodyPublisher body = call.body().map(sent -> {
            request.header("Content-Type", sent.contentType());
            return HttpRequest.BodyPublishers.ofByteArray(sent.bytes());
        }).orElse(HttpRequest.BodyPublishers.noBody());
        return request.method(call.method().name(), body).build();
    }

    private static HttpCallException failureOf(HttpCall call, Throwable cause) {
        if (cause instanceof HttpCallException known) {
            return known;
        }
        if (cause instanceof HttpTimeoutException) {
            return new HttpCallException(HttpCallException.Failure.TIMEOUT, "No answer from " + call.uri().getHost()
                    + " within " + call.timeout().toMillis() + " ms.");
        }
        return new HttpCallException(HttpCallException.Failure.TRANSPORT, "The call to " + call.uri().getHost() + " failed: "
                + (cause == null ? "no reason given" : cause.getClass().getSimpleName()) + ".");
    }

    @Override
    public void close() {
        client.close();
    }
}
