package org.jmouse.http.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("a call made by the JDK's client")
class JdkHttpTransportTest {

    private HttpServer server;
    private JdkHttpTransport transport;
    private final AtomicReference<String> received = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/echo", exchange -> {
            received.set(exchange.getRequestMethod() + " " + exchange.getRequestHeaders().getFirst("X-Token") + " "
                    + exchange.getRequestHeaders().getFirst("Content-Type") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            answer(exchange, 201, "made");
        });
        server.createContext("/large", exchange -> answer(exchange, 200, "x".repeat(10_000)));
        server.createContext("/slow", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write('a');
                body.flush();
                Thread.sleep(2_000);
                body.write('b');
            } catch (InterruptedException | IOException ignored) {
                // the client went away, as it should
            }
        });
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", "/echo");
            answer(exchange, 302, "");
        });
        server.start();
        transport = new JdkHttpTransport(Duration.ofSeconds(2), JdkHttpTransport.Redirects.NEVER);
    }

    @AfterEach
    void stop() {
        transport.close();
        server.stop(0);
    }

    private static void answer(com.sun.net.httpserver.HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    private URI at(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Test
    @DisplayName("the call arrives as made, and its answer comes back whole")
    void aCallAndItsAnswer() {
        HttpReply reply = transport.exchange(HttpCall.post(at("/echo")).header("X-Token", "secret")
                .text("{\"a\":1}", "application/json").build());
        assertEquals(201, reply.status());
        assertTrue(reply.successful());
        assertEquals("made", reply.text());
        assertEquals("POST secret application/json {\"a\":1}", received.get());
    }

    @Test
    @DisplayName("an answer longer than the call reads is refused")
    void anAnswerPastItsLimit() {
        HttpCallException refused = assertThrows(HttpCallException.class,
                () -> transport.exchange(HttpCall.get(at("/large")).responseLimit(1_000).build()));
        assertEquals(HttpCallException.Failure.RESPONSE_LIMIT, refused.failure());
    }

    @Test
    @DisplayName("the whole call is timed, its body's last byte included")
    void theWholeCallIsTimed() {
        HttpCallException late = assertThrows(HttpCallException.class,
                () -> transport.exchange(HttpCall.get(at("/slow")).timeout(Duration.ofMillis(500)).build()));
        assertEquals(HttpCallException.Failure.TIMEOUT, late.failure());
    }

    @Test
    @DisplayName("a redirect is an answer, not followed unless the transport was made to")
    void aRedirectIsAnAnswer() {
        HttpReply reply = transport.exchange(HttpCall.get(at("/moved")).build());
        assertEquals(302, reply.status());
        assertFalse(reply.successful());
        assertEquals("/echo", reply.header("location").orElseThrow());
    }

    @Test
    @DisplayName("a transport made to follow redirects says where the answer came from")
    void aFollowedRedirectSaysWhereItLed() {
        try (JdkHttpTransport following = new JdkHttpTransport(Duration.ofSeconds(2), JdkHttpTransport.Redirects.NORMAL)) {
            HttpReply reply = following.exchange(HttpCall.get(at("/moved")).build());
            assertEquals(201, reply.status());
            assertEquals(at("/echo"), reply.uri());
        }
    }

    @Test
    @DisplayName("a header that breaks a line is refused before anything is sent, its value never said")
    void aSmuggledHeaderIsRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> HttpCall.get(at("/echo")).header("X-API-Key", "secret\r\nX-Other: 1").build());
        assertFalse(refused.getMessage().contains("secret"));
        assertThrows(IllegalArgumentException.class, () -> HttpCall.get(at("/echo")).header("Bad Name", "1").build());
    }

    @Test
    @DisplayName("a call goes to an absolute address")
    void aRelativeAddressIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> HttpCall.get(URI.create("/echo")).build());
    }
}
