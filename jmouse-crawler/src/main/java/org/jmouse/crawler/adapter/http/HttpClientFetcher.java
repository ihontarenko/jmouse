package org.jmouse.crawler.adapter.http;

import org.jmouse.core.MediaType;
import org.jmouse.core.MimeParser;
import org.jmouse.core.Verify;
import org.jmouse.crawler.api.FetchRequest;
import org.jmouse.crawler.api.FetchResult;
import org.jmouse.crawler.api.Fetcher;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Fetcher} implementation based on Java's {@link HttpClient}. 🌐
 *
 * <p>This fetcher performs simple HTTP GET requests and returns a {@link FetchResult}
 * containing status, effective URI, headers, body bytes, and content type.</p>
 *
 * <p>Behavior is primarily driven by {@link HttpFetcherConfig}:</p>
 * <ul>
 *   <li>connect timeout</li>
 *   <li>request timeout</li>
 *   <li>redirect policy</li>
 *   <li>User-Agent header</li>
 * </ul>
 *
 * <p>Per-request headers from {@link FetchRequest#headers()} are appended on top
 * of the default headers.</p>
 */
public final class HttpClientFetcher implements Fetcher {

    private static final String HEADER_USER_AGENT   = "User-Agent";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final HttpClient        client;
    private final HttpFetcherConfig config;

    /**
     * Create a new {@code HttpClientFetcher}.
     *
     * @param config configuration for timeouts, redirects, and default headers
     */
    public HttpClientFetcher(HttpFetcherConfig config) {
        this.config = Verify.nonNull(config, "config");

        this.client = HttpClient.newBuilder()
                .connectTimeout(this.config.connectTimeout())
                .followRedirects(
                        this.config.followRedirects() ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Execute an HTTP GET request for the given {@link FetchRequest}.
     *
     * @param request fetch request (must be non-null)
     * @return fetch result with response details
     * @throws Exception if the underlying {@link HttpClient} request fails
     */
    @Override
    public FetchResult fetch(FetchRequest request) throws Exception {
        Verify.nonNull(request, "request");

        URI         uri         = Verify.nonNull(request.url(), "request.url");
        HttpRequest httpRequest = buildRequest(uri, request);

        HttpResponse<byte[]> response =
                client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

        return toResult(response);
    }

    /**
     * Build an {@link HttpRequest} for the provided URI and request metadata.
     */
    private HttpRequest buildRequest(URI uri, FetchRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(config.requestTimeout())
                .GET()
                .header(HEADER_USER_AGENT, config.userAgent());

        // Add request-specific headers (may override defaults depending on HttpClient behavior).
        for (Map.Entry<String, String> entry : request.headers().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /**
     * Convert the Java {@link HttpResponse} to a framework {@link FetchResult}.
     */
    private static FetchResult toResult(HttpResponse<byte[]> response) {
        String contentType = firstHeaderValue(response, HEADER_CONTENT_TYPE);
        return new FetchResult(
                response.uri(),
                response.statusCode(),
                flattenHeaders(response.headers().map()),
                response.body(),
                MediaType.forString(contentType),
                contentType
        );
    }

    /**
     * Read the first header value for the given name, or {@code null} if missing.
     */
    private static String firstHeaderValue(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse(null);
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
