package org.jmouse.ai.reranking;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The real transport against an in-process loopback server: byte cap, refusal, redirects and time. */
class JdkRerankTransportTest {

    private HttpServer         server;
    private JdkRerankTransport transport;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/small", exchange -> answer(exchange, 200, "[{\"index\":0,\"score\":0.5}]"));
        server.createContext("/large", exchange -> answer(exchange, 200, "[" + "{\"index\":0,\"score\":0.5},".repeat(400) + "{}]"));
        server.createContext("/refused", exchange -> answer(exchange, 429, "{\"secret\":\"provider text\"}"));
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://elsewhere.example/");
            answer(exchange, 302, "");
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            answer(exchange, 200, "[]");
        });
        server.start();
        transport = new JdkRerankTransport(Duration.ofSeconds(2));
    }

    @AfterEach
    void stop() {
        transport.close();
        server.stop(0);
    }

    private static void answer(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private URI address(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private final RerankLimits limits = new RerankLimits(10, 100, 100, 1000, 1000, 1024);

    @Test
    void readsJsonWithinTheCap() {
        assertEquals(List.of(Map.of("index", 0, "score", 0.5)), transport.post(address("/small"), Map.of(), Map.of(), limits));
    }

    @Test
    void refusesABodyOverTheCapBeforeParsingIt() {
        var failure = assertThrows(RerankException.class, () -> transport.post(address("/large"), Map.of(), Map.of(), limits));
        assertEquals(RerankException.Reason.RESPONSE_LIMIT, failure.reason());
    }

    @Test
    void aProviderRefusalCarriesTheStatusButNotTheBody() {
        var failure = assertThrows(RerankException.class, () -> transport.post(address("/refused"), Map.of(), Map.of(), limits));
        assertEquals(RerankException.Reason.PROVIDER_REFUSED, failure.reason());
        assertEquals(429, failure.status());
        assertTrue(failure.retryable());
        assertFalse(failure.getMessage().contains("provider text"));
    }

    @Test
    void neverFollowsARedirect() {
        var failure = assertThrows(RerankException.class, () -> transport.post(address("/moved"), Map.of(), Map.of(), limits));
        assertEquals(RerankException.Reason.PROVIDER_REFUSED, failure.reason());
        assertEquals(302, failure.status());
    }

    @Test
    void timesOutAndCancels() {
        var failure = assertThrows(RerankException.class, () -> transport.post(address("/slow"), Map.of(), Map.of(), limits));
        assertEquals(RerankException.Reason.TIMEOUT, failure.reason());
    }
}
