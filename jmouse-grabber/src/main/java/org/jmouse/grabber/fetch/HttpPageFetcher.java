package org.jmouse.grabber.fetch;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jmouse.core.MediaType;
import org.jmouse.grabber.GrabberException;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpStatus;

/**
 * 🌐 The standard fetcher, over {@link HttpClient}.
 *
 * <p>The library ships no HTTP client abstraction of its own — {@code jmouse-http} is value types —
 * so this class is where the JDK's client is wrapped, once, behind {@link PageFetcher}. Everything
 * that would otherwise be repeated in a handler lives here: redirects followed with the final address
 * reported, the charset decided, headers translated in both directions.</p>
 */
public final class HttpPageFetcher implements PageFetcher {

    private final HttpClient client;
    private final Settings   settings;

    HttpPageFetcher(Settings settings) {
        this.settings = settings;
        this.client = HttpClient.newBuilder()
                .connectTimeout(settings.connectTimeout())
                .followRedirects(settings.followRedirects()
                                         ? HttpClient.Redirect.NORMAL
                                         : HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public PageResponse fetch(PageRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.address())
                .timeout(request.timeout() == null ? settings.requestTimeout() : request.timeout());

        applyHeaders(builder, request.headers());
        applyMethod(builder, request);

        long start = System.nanoTime();

        try {
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

            return new PageResponse(
                    response.uri(),
                    request.address(),
                    HttpStatus.ofCode(response.statusCode()),
                    readHeaders(response),
                    response.body(),
                    Duration.ofNanos(System.nanoTime() - start)
            );
        } catch (IOException exception) {
            throw new GrabberException("Cannot fetch " + request.address() + ": " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GrabberException("Interrupted while fetching " + request.address(), exception);
        }
    }

    private void applyMethod(HttpRequest.Builder builder, PageRequest request) {
        HttpRequest.BodyPublisher body = request.hasBody()
                ? HttpRequest.BodyPublishers.ofByteArray(request.body())
                : HttpRequest.BodyPublishers.noBody();

        builder.method(request.method().name(), body);
    }

    private void applyHeaders(HttpRequest.Builder builder, Headers headers) {
        for (Map.Entry<HttpHeader, Object> entry : headers.asMap().entrySet()) {
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            if (isRestricted(entry.getKey())) {
                continue;
            }

            builder.header(entry.getKey().value(), String.valueOf(value));
        }
    }

    /**
     * 🚫 Headers the JDK client refuses to let a caller set, throwing if one tries.
     *
     * <p>Dropping them silently is right here: a run's standing headers are written once for every
     * site it will ever visit, and one of them being a name the client manages itself should not
     * fail every fetch.</p>
     */
    private boolean isRestricted(HttpHeader header) {
        return header == HttpHeader.HOST
                || header == HttpHeader.CONNECTION
                || header == HttpHeader.CONTENT_LENGTH
                || header == HttpHeader.UPGRADE;
    }

    private Headers readHeaders(HttpResponse<byte[]> response) {
        Headers headers = new Headers();

        response.headers().map().forEach((name, values) -> {
            if (values.isEmpty()) {
                return;
            }

            HttpHeader header = HttpHeader.ofHeader(name);

            if (header == null) {
                return;
            }

            if (header == HttpHeader.CONTENT_TYPE) {
                headers.setContentType(MediaType.forString(values.getFirst()));
            } else {
                headers.setHeader(header, single(values));
            }
        });

        headers.setStatus(HttpStatus.ofCode(response.statusCode()));

        return headers;
    }

    private String single(List<String> values) {
        return values.size() == 1 ? values.getFirst() : String.join(", ", values);
    }

    /*
     * ⚠️ A response header that jmouse-http's enum does not name is dropped, because Headers is keyed
     * by that enum and there is nowhere else to put one. This costs nothing for the headers a grabber
     * reads — content type, location, caching, entity tags are all there — and it does mean a site's
     * own X-* header is invisible. Reading one means adding it to HttpHeader, which is the right place
     * for it and a change to jmouse-http rather than a workaround here.
     */

    /**
     * ⚙️ What the fetcher itself needs to know, as opposed to what a run's policy decides.
     *
     * <p>⚠️ The split matters: the request timeout here is a default the policy overrides per
     * request, while the connect timeout and the redirect behaviour are properties of the client and
     * cannot vary per call.</p>
     */
    public record Settings(Duration connectTimeout, Duration requestTimeout, boolean followRedirects) {

        public static Settings defaults() {
            return new Settings(Duration.ofSeconds(10), Duration.ofSeconds(30), true);
        }

        public Settings connectingWithin(Duration timeout) {
            return new Settings(timeout, requestTimeout, followRedirects);
        }

        public Settings requestingWithin(Duration timeout) {
            return new Settings(connectTimeout, timeout, followRedirects);
        }

        /**
         * 🔁 Stops following redirects, so a 301 arrives as a response to be decided on.
         */
        public Settings withoutFollowingRedirects() {
            return new Settings(connectTimeout, requestTimeout, false);
        }
    }

}
