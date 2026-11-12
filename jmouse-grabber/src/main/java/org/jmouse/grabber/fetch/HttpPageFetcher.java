package org.jmouse.grabber.fetch;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpCallException;
import org.jmouse.http.client.HttpReply;
import org.jmouse.http.client.JdkHttpTransport;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jmouse.core.MediaType;
import org.jmouse.grabber.GrabberException;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpStatus;

/**
 * 🌐 The standard fetcher, over the jMouse HTTP client ({@link JdkHttpTransport}).
 *
 * <p>The call itself — the whole fetch timed, the page read within its size, redirects followed with the final address
 * reported, the headers the JDK writes itself left to it — is the client's. What lives here is the grabber's: a request
 * and a response as its {@link PageRequest} and {@link PageResponse}, headers translated in both directions.</p>
 */
public final class HttpPageFetcher implements PageFetcher {

    /** What a body is said to be when its request's headers do not say. */
    private static final String OCTETS = "application/octet-stream";

    private final JdkHttpTransport transport;
    private final Settings         settings;

    HttpPageFetcher(Settings settings) {
        this.settings = settings;
        this.transport = new JdkHttpTransport(settings.connectTimeout(), settings.redirects());
    }

    @Override
    public PageResponse fetch(PageRequest request) {
        long start = System.nanoTime();
        try {
            HttpReply reply = transport.exchange(callOf(request));
            return new PageResponse(reply.uri(), request.address(), HttpStatus.ofCode(reply.status()), readHeaders(reply),
                    reply.body(), Duration.ofNanos(System.nanoTime() - start));
        } catch (HttpCallException exception) {
            if (exception.failure() == HttpCallException.Failure.INTERRUPTED) {
                throw new GrabberException("Interrupted while fetching " + request.address(), exception);
            }
            throw new GrabberException("Cannot fetch " + request.address() + ": " + exception.getMessage(), exception);
        }
    }

    /** The request as a call: its method, its headers, and its body of the type its own headers say. */
    private HttpCall callOf(PageRequest request) {
        HttpCall.Builder call = HttpCall.to(request.method(), request.address())
                .timeout(request.timeout() == null ? settings.requestTimeout() : request.timeout())
                .responseLimit(settings.maximumResponseBytes());
        for (Map.Entry<HttpHeader, Object> entry : request.headers().asMap().entrySet()) {
            if (entry.getValue() != null && !(request.hasBody() && entry.getKey() == HttpHeader.CONTENT_TYPE)) {
                call.header(entry.getKey().value(), String.valueOf(entry.getValue()));
            }
        }
        if (request.hasBody()) {
            Object type = request.headers().getHeader(HttpHeader.CONTENT_TYPE);
            call.body(request.body(), type == null ? OCTETS : String.valueOf(type));
        }
        return call.build();
    }

    private Headers readHeaders(HttpReply reply) {
        Headers headers = new Headers();

        reply.headers().forEach((name, values) -> {
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

        headers.setStatus(HttpStatus.ofCode(reply.status()));

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
    public record Settings(Duration connectTimeout, Duration requestTimeout, JdkHttpTransport.Redirects redirects,
                           long maximumResponseBytes) {

        public static Settings defaults() {
            return new Settings(Duration.ofSeconds(10), Duration.ofSeconds(30), JdkHttpTransport.Redirects.NORMAL,
                    HttpCall.USUAL_RESPONSE_LIMIT);
        }

        public Settings connectingWithin(Duration timeout) {
            return new Settings(timeout, requestTimeout, redirects, maximumResponseBytes);
        }

        public Settings requestingWithin(Duration timeout) {
            return new Settings(connectTimeout, timeout, redirects, maximumResponseBytes);
        }

        /** 📏 Reads a page up to this many bytes; past them the fetch fails. */
        public Settings readingAtMost(long bytes) {
            return new Settings(connectTimeout, requestTimeout, redirects, bytes);
        }

        /**
         * 🔁 Stops following redirects, so a 301 arrives as a response to be decided on.
         */
        public Settings withoutFollowingRedirects() {
            return new Settings(connectTimeout, requestTimeout, JdkHttpTransport.Redirects.NEVER, maximumResponseBytes);
        }
    }

}
