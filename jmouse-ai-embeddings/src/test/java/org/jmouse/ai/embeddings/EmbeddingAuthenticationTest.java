package org.jmouse.ai.embeddings;

import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Custom authentication is delivered over HTTP and never silently ignored by older transports. */
class EmbeddingAuthenticationTest {

    @Test
    void protocolHeadersReachTheProviderWithoutAnImplicitBearerCredential() throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/embed", exchange -> {
            calls.incrementAndGet();
            boolean authenticated = "fixture-secret".equals(exchange.getRequestHeaders().getFirst("X-API-Key"))
                    && exchange.getRequestHeaders().getFirst("Authorization") == null;
            exchange.getRequestBody().readAllBytes();
            byte[] body = "[[1,2]]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(authenticated ? 200 : 401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        var settings = new ProviderSettings("custom-auth-fixture", "fixture-model", "fixture-secret",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/embed", 100,
                Set.of(AiCapability.EMBEDDINGS), "fixture", 1);
        var request = new EmbeddingModel.Request(List.of("JDBC"), 2);
        var limits = new CallLimits(1, 100, 100, 2000, 4096);
        var protocol = new CustomAuthenticationProtocol();

        try (var transport = new JdkEmbeddingTransport(Duration.ofSeconds(1))) {
            var response = new HttpEmbeddingModel(protocol, transport).embed(settings, request, limits);
            assertEquals(List.of(List.of(1.0, 2.0)), response.vectors());
            assertEquals(1, calls.get());

            EmbeddingTransport legacy = (endpoint, provider, body, bounds) -> {
                fail("A legacy transport must refuse unsupported custom headers before I/O.");
                return null;
            };
            assertEquals(EmbeddingException.Reason.CONFIGURATION,
                    assertThrows(EmbeddingException.class,
                            () -> new HttpEmbeddingModel(protocol, legacy).embed(settings, request, limits)).reason());
            var failure = assertThrows(EmbeddingException.class,
                    () -> transport.post(java.net.URI.create(settings.apiUrl()), settings, Map.of(),
                            Map.of("X-API-Key", "fixture-secret\r\ninvalid"), limits));
            assertEquals(EmbeddingException.Reason.CONFIGURATION, failure.reason());
            assertFalse(failure.getMessage().contains("fixture-secret"));
            assertEquals(1, calls.get());
        } finally {
            server.stop(0);
        }
    }

    private static final class CustomAuthenticationProtocol extends AbstractEmbeddingProtocol {

        private CustomAuthenticationProtocol() {
            super("CUSTOM_AUTH_FIXTURE", Set.of("custom-auth-fixture"), Set.of("custom-auth-fixture"));
        }

        @Override
        public Map<String, String> headers(ProviderSettings settings) {
            return Map.of("X-API-Key", settings.apiKey());
        }

        @Override
        public Object encode(ProviderSettings settings, EmbeddingModel.Request request) {
            return Map.of("texts", request.inputs());
        }

        @Override
        public EmbeddingModel.Response decode(Object response, ProviderSettings settings, EmbeddingModel.Request request) {
            return new EmbeddingModel.Response(EmbeddingResponseReader.vectors(response, request), null);
        }
    }
}
