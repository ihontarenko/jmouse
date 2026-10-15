package org.jmouse.ai.provider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class BoundedChatResponseTest {

    @Test
    void countsUtf8BytesAndAcceptsTheExactBoundaryAcrossSplitBuffers() {
        byte[] bytes = "Київ😀".getBytes(StandardCharsets.UTF_8);
        var cancelled = new AtomicBoolean();
        var body = new BoundedChatResponse(bytes.length).apply(information(Map.of()));
        body.onSubscribe(subscription(cancelled));
        body.onNext(List.of(ByteBuffer.wrap(bytes, 0, 3)));
        body.onNext(List.of(ByteBuffer.wrap(bytes, 3, bytes.length - 3)));
        body.onComplete();
        assertEquals("Київ😀", body.getBody().toCompletableFuture().join());
        assertFalse(cancelled.get());
    }

    @Test
    void cancelsChunkedOverflowBeforeForwardingTheOffendingBatch() {
        var cancelled = new AtomicBoolean();
        var body = new BoundedChatResponse(4).apply(information(Map.of()));
        body.onSubscribe(subscription(cancelled));
        body.onNext(List.of(ByteBuffer.wrap(new byte[4])));
        body.onNext(List.of(ByteBuffer.wrap(new byte[1])));
        body.onComplete();
        var failure = assertThrows(CompletionException.class, () -> body.getBody().toCompletableFuture().join());
        assertInstanceOf(ChatResponseLimitException.class, failure.getCause());
        assertTrue(cancelled.get());
    }

    @Test
    void rejectsOversizedDeclaredLengthWithoutAcceptingBodyBytes() {
        var cancelled = new AtomicBoolean();
        var body = new BoundedChatResponse(4).apply(information(Map.of("Content-Length", List.of("5"))));
        body.onSubscribe(subscription(cancelled));
        var failure = assertThrows(CompletionException.class, () -> body.getBody().toCompletableFuture().join());
        assertEquals(4, assertInstanceOf(ChatResponseLimitException.class, failure.getCause()).maximumResponseBytes());
        assertTrue(cancelled.get());
    }

    @Test
    void cancellationAlsoBoundsNativeHttpErrorAndChunkedResponses() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().equals("/error") ? 500 : 200, 0);
            try {
                exchange.getResponseBody().write(new byte[1025]);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            for (String endpoint : List.of("/error", "/success")) {
                var settings = new ProviderSettings("ollama", "fixture", null, "http://127.0.0.1:" + server.getAddress().getPort() + endpoint, 20);
                var model = ProviderCatalog.modelFor("ollama", ProviderSettingsSource.fixed(settings),
                        new ChatHttpPolicy(Duration.ofSeconds(1), Duration.ofSeconds(2), 1024)).orElseThrow();
                assertThrows(ChatResponseLimitException.class, () -> model.converse(ChatRequest.of(List.of())));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void validatesLimitsAndLeavesLegacyFactoriesAvailable() {
        assertThrows(IllegalArgumentException.class, () -> new ChatHttpPolicy(Duration.ZERO, Duration.ofSeconds(1), 1));
        assertThrows(IllegalArgumentException.class, () -> new ChatHttpPolicy(Duration.ofSeconds(1), Duration.ofSeconds(1), 0));
        var source = ProviderSettingsSource.fixed(ProviderSettings.of("ollama", "fixture", null, 20));
        assertTrue(ProviderCatalog.modelFor("ollama", source).isPresent());
        assertTrue(ProviderCatalog.modelFor("ollama", source, Duration.ofSeconds(1), Duration.ofSeconds(1)).isPresent());
        assertTrue(ProviderCatalog.modelFor("voyage", source, new ChatHttpPolicy(Duration.ofSeconds(1), Duration.ofSeconds(1), 1024)).isEmpty());
    }

    @Test
    void responseDeadlineCoversAStallAfterHeadersAndPartialBody() throws Exception {
        var release = new java.util.concurrent.CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, 0);
            try {
                exchange.getResponseBody().write('{');
                exchange.getResponseBody().flush();
                release.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            var settings = new ProviderSettings("ollama", "fixture", null, "http://127.0.0.1:" + server.getAddress().getPort(), 20);
            var model = ProviderCatalog.modelFor("ollama", ProviderSettingsSource.fixed(settings),
                    new ChatHttpPolicy(Duration.ofSeconds(1), Duration.ofMillis(200), 1024)).orElseThrow();
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertThrows(ProviderException.class,
                    () -> model.converse(ChatRequest.of(List.of()))));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    private static Flow.Subscription subscription(AtomicBoolean cancelled) {
        return new Flow.Subscription() {
            public void request(long count) { }
            public void cancel() { cancelled.set(true); }
        };
    }

    private static HttpResponse.ResponseInfo information(Map<String, List<String>> headers) {
        return new HttpResponse.ResponseInfo() {
            public int statusCode() { return 200; }
            public HttpHeaders headers() { return HttpHeaders.of(headers, (name, value) -> true); }
            public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }
}
