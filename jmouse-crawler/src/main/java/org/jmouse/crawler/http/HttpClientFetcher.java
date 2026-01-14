package org.jmouse.crawler.http;

import org.jmouse.core.Verify;
import org.jmouse.crawler.spi.FetchRequest;
import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.Fetcher;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpClientFetcher implements Fetcher {

    private final HttpClient client;
    private final HttpFetcherConfig config;

    public HttpClientFetcher(HttpFetcherConfig config) {
        this.config = Verify.nonNull(config, "config");

        this.client = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(
                        config.followRedirects()
                                ? HttpClient.Redirect.NORMAL
                                : HttpClient.Redirect.NEVER
                )
                .build();
    }

    @Override
    public FetchResult fetch(FetchRequest request) throws Exception {
        Verify.nonNull(request, "request");

        URI uri = Verify.nonNull(request.url(), "request.url");

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(config.requestTimeout())
                .GET()
                .header("User-Agent", config.userAgent());

        // custom headers
        for (Map.Entry<String, String> entry : request.headers().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest httpRequest = builder.build();

        HttpResponse<byte[]> response =
                client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

        return new FetchResult(
                response.uri(),
                response.statusCode(),
                flattenHeaders(response.headers().map()),
                response.body(),
                contentType(response)
        );
    }

    private static String contentType(HttpResponse<?> response) {
        return response.headers()
                .firstValue("Content-Type")
                .orElse(null);
    }

    private static Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.put(entry.getKey(), entry.getValue().getFirst());
            }
        }

        return result;
    }
}
