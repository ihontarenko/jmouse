package org.jmouse.crawler.adapter.http;

import org.jmouse.core.MediaType;
import org.jmouse.core.Verify;
import org.jmouse.crawler.api.FetchRequest;
import org.jmouse.crawler.api.FetchResult;
import org.jmouse.crawler.api.Fetcher;
import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpCallException;
import org.jmouse.http.client.HttpReply;
import org.jmouse.http.client.JdkHttpTransport;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Fetcher} implementation over the jMouse HTTP client ({@link JdkHttpTransport}). 🌐
 *
 * <p>This fetcher performs simple HTTP GET requests and returns a {@link FetchResult}
 * containing status, effective URI, headers, body bytes, and content type.</p>
 *
 * <p>Behavior is primarily driven by {@link HttpFetcherConfig}:</p>
 * <ul>
 *   <li>connect timeout</li>
 *   <li>request timeout</li>
 *   <li>redirect policy</li>
 *   <li>how much of a page is read</li>
 *   <li>User-Agent header</li>
 * </ul>
 *
 * <p>Per-request headers from {@link FetchRequest#headers()} are appended on top
 * of the default headers.</p>
 */
public final class HttpClientFetcher implements Fetcher {

    private static final String HEADER_USER_AGENT   = "User-Agent";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final JdkHttpTransport  transport;
    private final HttpFetcherConfig config;

    /**
     * Create a new {@code HttpClientFetcher}.
     *
     * @param config configuration for timeouts, redirects, the page size read, and default headers
     */
    public HttpClientFetcher(HttpFetcherConfig config) {
        this.config = Verify.nonNull(config, "config");
        this.transport = new JdkHttpTransport(this.config.connectTimeout(), this.config.redirects());
    }

    /**
     * Execute an HTTP GET request for the given {@link FetchRequest}: the whole fetch timed, the page read within the
     * configured size.
     *
     * @param request fetch request (must be non-null)
     * @return fetch result with response details
     * @throws HttpCallException when no answer came: too slow, too long, the connection failed
     */
    @Override
    public FetchResult fetch(FetchRequest request) {
        Verify.nonNull(request, "request");
        URI uri = Verify.nonNull(request.url(), "request.url");
        // Request-specific headers are added after the user agent, so a request may name its own.
        HttpCall call = HttpCall.get(uri).header(HEADER_USER_AGENT, config.userAgent()).headers(request.headers())
                .timeout(config.requestTimeout()).responseLimit(config.maximumResponseBytes()).build();
        return toResult(transport.exchange(call));
    }

    /** The answer as a framework {@link FetchResult}. */
    private static FetchResult toResult(HttpReply reply) {
        String contentType = reply.header(HEADER_CONTENT_TYPE).orElse(null);
        return new FetchResult(reply.uri(), reply.status(), flattenHeaders(reply.headers()), reply.body(),
                MediaType.forString(contentType), contentType);
    }

    /**
     * Convert {@code Map<String, List<String>>} headers into a simple {@code Map<String, String>}
     * by taking the first value per header.
     *
     * <p>Useful for compact storage and simple downstream usage, but note that
     * it loses multi-value header information.</p>
     */
    private static Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new HashMap<>(Math.max(16, headers.size()));

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                result.put(entry.getKey(), values.getFirst());
            }
        }

        return result;
    }
}
