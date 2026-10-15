package org.jmouse.git.hosting;

import com.sun.net.httpserver.HttpExchange;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** The real transport against an in-process loopback server: byte cap, refusals, redirects, headers and time. */
class JdkHostingTransportTest {

    private final HostingLimits           limits         = new HostingLimits(1000, 1024);
    private final AtomicReference<String> authorisation  = new AtomicReference<>();
    private HttpServer                    server;
    private JdkHostingTransport           transport;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/small", exchange -> {
            authorisation.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().add("x-ratelimit-remaining", "41");
            answer(exchange, 200, "{\"login\":\"octocat\"}");
        });
        server.createContext("/large", exchange -> answer(exchange, 200, "[" + "{\"name\":\"repository\"},".repeat(200) + "{}]"));
        server.createContext("/refused", exchange -> {
            exchange.getResponseHeaders().add("retry-after", "60");
            answer(exchange, 429, "{\"message\":\"provider text\"}");
        });
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://elsewhere.example/");
            answer(exchange, 301, "");
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            answer(exchange, 200, "{}");
        });
        server.start();
        transport = new JdkHostingTransport(Duration.ofSeconds(2));
    }

    @AfterEach
    void stop() {
        transport.close();
        server.stop(0);
    }

    private static void answer(HttpExchange exchange, int status, String body) throws IOException {
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

    @Test
    void readsJsonAndHeadersWithinTheCapAndSendsTheHeadersGiven() {
        HostingHttpResponse response = transport.get(address("/small"), Map.of("Authorization", "Bearer token-value"), limits);

        assertEquals(200, response.status());
        assertEquals(Map.of("login", "octocat"), response.body());
        assertEquals(41L, RateLimit.of(response.headers()).remaining());
        assertEquals("Bearer token-value", authorisation.get());
    }

    @Test
    void refusesABodyOverTheCapBeforeParsingIt() {
        HostingException failure = assertThrows(HostingException.class, () -> transport.get(address("/large"), Map.of(), limits));

        assertEquals(HostingException.Reason.RESPONSE_LIMIT, failure.reason());
    }

    @Test
    void aRefusalComesBackWithItsHeadersAndWithoutItsBody() {
        HostingHttpResponse response = transport.get(address("/refused"), Map.of(), limits);

        assertEquals(429, response.status());
        assertNull(response.body());
        assertEquals(60, RateLimit.of(response.headers()).retryAfter().toSeconds());
    }

    @Test
    void neverFollowsARedirect() {
        HostingHttpResponse response = transport.get(address("/moved"), Map.of("Authorization", "Bearer token-value"), limits);

        assertEquals(301, response.status());
    }

    @Test
    void timesOutAndCancels() {
        HostingException failure = assertThrows(HostingException.class, () -> transport.get(address("/slow"), Map.of(), limits));

        assertEquals(HostingException.Reason.TIMEOUT, failure.reason());
        assertTrue(failure.retryable());
    }

    @Test
    void anInvalidHeaderValueIsRefusedWithoutRepeatingIt() {
        HostingException failure = assertThrows(HostingException.class,
                () -> transport.get(address("/small"), Map.of("Authorization", "Bearer secret\nvalue"), limits));

        assertEquals(HostingException.Reason.CONFIGURATION, failure.reason());
        assertFalse(failure.getMessage().contains("secret"));
    }
}
