package org.jmouse.ai.sandbox.provider;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Three providers' answers, from one socket, without an account anywhere.
 *
 * <p>What this is for is the half of a {@code ChatModel} that cannot be read off the source: the body
 * it actually puts on the wire. A canned answer is easy to fake in a test double; the request is not
 * fakeable at all without something on the other end of a connection, and the request is where the
 * translation either works or quietly does not.
 *
 * <p>The JDK's own server, on an ephemeral port. A sandbox that needed a container to demonstrate a
 * round trip would be a sandbox nobody runs.
 */
public final class StubProvider implements AutoCloseable {

    private final HttpServer          server;
    private final Map<String, String> received = new LinkedHashMap<>();

    private StubProvider(HttpServer server) {
        this.server = server;
    }

    /** Each provider on its own path, answering in its own shape. */
    public static StubProvider started() {
        try {
            HttpServer  server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            StubProvider stub  = new StubProvider(server);

            server.createContext("/anthropic", exchange -> stub.answer(exchange, ANTHROPIC_ANSWER));
            server.createContext("/openai",    exchange -> stub.answer(exchange, OPENAI_ANSWER));
            server.createContext("/gateway",   exchange -> stub.answer(exchange, GATEWAY_ANSWER));
            server.start();

            return stub;

        } catch (IOException unstartable) {
            throw new IllegalStateException("The stub provider could not take a port", unstartable);
        }
    }

    public String urlFor(String provider) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/" + provider;
    }

    /** What that provider was actually sent, as it arrived. */
    public String requestSentTo(String provider) {
        return received.getOrDefault(provider, "(nothing)");
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void answer(HttpExchange exchange, String body) throws IOException {
        try (InputStream request = exchange.getRequestBody()) {
            received.put(exchange.getHttpContext().getPath().substring(1),
                    new String(request.readAllBytes(), StandardCharsets.UTF_8));
        }

        byte[] answer = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("content-type", "application/json");
        exchange.sendResponseHeaders(200, answer.length);

        try (OutputStream response = exchange.getResponseBody()) {
            response.write(answer);
        }
    }

    // ── One answer, said three ways ──────────────────────────────────────────────

    /**
     * The same turn in each provider's own shape: a sentence, then a call to {@code parts_list} with
     * one argument, having spent the same tokens. Anything the three {@code ChatResponse}s disagree
     * about afterwards is a translation that is wrong.
     */
    private static final String ANTHROPIC_ANSWER = """
            {"stop_reason": "tool_use",
             "content": [{"type": "text", "text": "Let me look at that shelf."},
                         {"type": "tool_use", "id": "call-1", "name": "parts_list",
                          "input": {"shelf": "A"}}],
             "usage": {"input_tokens": 412, "output_tokens": 38}}
            """;

    private static final String OPENAI_ANSWER = """
            {"choices": [{"finish_reason": "tool_calls",
                          "message": {"role": "assistant",
                                      "content": "Let me look at that shelf.",
                                      "tool_calls": [{"id": "call-1", "type": "function",
                                                      "function": {"name": "parts_list",
                                                                   "arguments": "{\\"shelf\\": \\"A\\"}"}}]}}],
             "usage": {"prompt_tokens": 412, "completion_tokens": 38}}
            """;

    private static final String GATEWAY_ANSWER = """
            {"stopReason": "tool_use",
             "content": [{"type": "text", "text": "Let me look at that shelf."},
                         {"type": "tool_use", "id": "call-1", "name": "parts_list",
                          "input": {"shelf": "A"}}],
             "usage": {"inputTokens": 412, "outputTokens": 38}}
            """;
}
