package org.jmouse.ai.embeddings;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP protocol boundary, Unicode, ordering, capability refusals and bounded malformed responses. */
class EmbeddingProtocolTest {
    @Test
    void vectorizationAndPreflightStaySeparateAndBounded() throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            String input = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String response = switch (exchange.getRequestURI().getPath()) {
                case "/openai" -> "{\"model\":\"model-v1\",\"data\":[{\"index\":1,\"embedding\":[3,4]},{\"index\":0,\"embedding\":[1,2]}],\"usage\":{\"prompt_tokens\":9}}";
                case "/ollama" -> input.contains("\"truncate\":false")
                        ? "{\"model\":\"model-v1\",\"embeddings\":[[1,2],[3,4]],\"prompt_eval_count\":9}" : "{}";
                case "/embed" -> "[[1,2],[3,4]]";
                case "/tokenize" -> "[[{\"id\":1},{\"id\":2}],[{\"id\":1},{\"id\":2},{\"id\":3}]]";
                case "/broken" -> "{\"model\":\"model-v1\",\"data\":[{\"index\":0,\"embedding\":[1,2]},{\"index\":0,\"embedding\":[3,4]}]}";
                case "/large" -> "x".repeat(4096);
                case "/redirect" -> "{}";
                default -> "{}";
            };
            if (exchange.getRequestURI().getPath().equals("/redirect")) {
                exchange.getResponseHeaders().add("Location", "/embed");
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().equals("/redirect") ? 302 : 200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        var limits = new CallLimits(4, 1000, 4000, 2000, 1024);
        var request = new EmbeddingModel.Request(List.of("Україна 😀", "JDBC"), 2);
        try (var transport = new JdkEmbeddingTransport(Duration.ofSeconds(1))) {
            var openai = new ProtocolEmbeddingModel(ProtocolEmbeddingModel.Protocol.OPENAI, transport);
            var settings = settings("openai", base + "/openai");
            var answer = openai.embed(settings, request, limits);
            assertEquals(List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)), answer.vectors());
            assertEquals(9L, answer.inputTokens());
            assertThrows(UnsupportedOperationException.class, () -> answer.vectors().getFirst().add(5.0));
            int beforeRefusal = calls.get();
            assertThrows(org.jmouse.ai.provider.ProviderException.class,
                    () -> openai.embed(new ProviderSettings("openai", "model-v1", "secret", base + "/openai", 100), request, limits));
            assertEquals(beforeRefusal, calls.get());
            assertFalse(settings.toString().contains("secret"));
            assertEquals(9L, new ProtocolEmbeddingModel(ProtocolEmbeddingModel.Protocol.OLLAMA, transport)
                    .embed(settings("ollama", base + "/ollama"), request, limits).inputTokens());
            assertNull(new ProtocolEmbeddingModel(ProtocolEmbeddingModel.Protocol.TEI, transport)
                    .embed(settings("tei", base + "/embed"), request, limits).inputTokens());
            var counter = new ManagedTokenCounter(transport);
            int beforeCounts = calls.get();
            var exact = counter.count(settings("tei", base + "/embed"), request.inputs(),
                    new TokenCounter.Policy(TokenCounter.Strategy.TEI, "model-v1", "immutable-v1", URI.create(base + "/tokenize"), 1, 0), limits);
            assertEquals(List.of(2L, 3L), exact.perInput());
            assertEquals(TokenCounter.Quality.EXACT, exact.quality());
            assertEquals(beforeCounts + 1, calls.get());
            var estimate = counter.count(settings, request.inputs(),
                    new TokenCounter.Policy(TokenCounter.Strategy.CODE_POINT_ESTIMATE, "estimated", "policy-v1", null, 2, 1), limits);
            assertEquals(TokenCounter.Quality.APPROXIMATE, estimate.quality());
            assertEquals(beforeCounts + 1, calls.get());
            var unknown = counter.count(settings, request.inputs(),
                    new TokenCounter.Policy(TokenCounter.Strategy.UNKNOWN, "unknown", "unknown", null, 1, 0), limits);
            assertEquals(TokenCounter.Quality.UNKNOWN, unknown.quality());
            assertNull(unknown.perInput().getFirst());
            assertThrows(EmbeddingException.class, () -> openai.embed(settings("openai", base + "/broken"), request, limits));
            assertEquals(EmbeddingException.Reason.RESPONSE_LIMIT,
                    assertThrows(EmbeddingException.class, () -> openai.embed(settings("openai", base + "/large"), request, limits)).reason());
            int beforeRedirect = calls.get();
            assertEquals(302, assertThrows(EmbeddingException.class, () -> openai.embed(settings("openai", base + "/redirect"), request, limits)).status());
            assertEquals(beforeRedirect + 1, calls.get());
            assertThrows(IllegalArgumentException.class, () -> openai.embed(settings, request, new CallLimits(1, 1000, 4000, 2000, 1024)));
        } finally {
            server.stop(0);
        }
    }
    private static ProviderSettings settings(String provider, String endpoint) {
        return new ProviderSettings(provider, "model-v1", "secret", endpoint, 100,
                Set.of(AiCapability.EMBEDDINGS), "configuration", 1);
    }
}

