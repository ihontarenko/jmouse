package org.jmouse.telegram.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Everything that is the same about calling any Bot API method.
 *
 * <p>A method is then three small things — its name, the fields it sends, and how to read the reply —
 * and none of them knows about timeouts, JSON, HTTP status codes, credentials or what an unreachable
 * host looks like. ⚠️ That split is the requirement, not an aesthetic: if adding {@code sendVenue}
 * means touching anything in this class, the split has been got wrong and the twentieth method will be
 * a twentieth copy of the error handling.
 *
 * <p>{@code HttpChatModel} in {@code jmouse-ai-provider} is the same arrangement, and this follows it
 * deliberately.
 *
 * <h2>Timeouts</h2>
 *
 * <p>Both finite, and set here rather than left to a caller. A hung endpoint otherwise holds the
 * calling thread forever, and enough of those exhaust a server's pool — at which point the failure
 * looks like the whole application being down rather than one integration being slow.
 *
 * <p>⚠️ The read timeout is a parameter because long polling legitimately needs a much longer one: a
 * {@code getUpdates} call is <em>supposed</em> to sit open for a minute waiting for something to
 * happen, and a timeout tuned for sending would abort it every time.
 *
 * <h2>The three ways this fails</h2>
 *
 * <p>All three become a {@link TelegramException} carrying a {@link TelegramRefusal}, and they are
 * separated because they have three different fixes: Telegram said no (translated by
 * {@link BotApiErrors}), nothing answered at all (refused, timed out, DNS), or the configured address
 * was never an address — which arrives, unhelpfully, as an {@link IllegalArgumentException} thrown
 * before any request is made.
 */
public abstract class BotApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotApiClient.class);

    /** Long enough for a slow handshake, short enough that a black hole is noticed. */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /** Generous for a send, and deliberately not long enough for a polling call. */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    protected final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient httpClient;
    private final Duration   readTimeout;

    protected BotApiClient() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected BotApiClient(Duration connectTimeout, Duration readTimeout) {
        this.httpClient  = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.readTimeout = readTimeout;
    }

    /**
     * Calls a method whose fields are all values — the ordinary case.
     *
     * @return the {@code result} member of Telegram's reply
     */
    protected JsonNode call(TelegramIdentity identity, String method, Map<String, Object> fields) {
        return call(identity, method, fields, readTimeout);
    }

    /** As {@link #call(TelegramIdentity, String, Map)}, for a call that is expected to wait. */
    protected JsonNode call(
            TelegramIdentity identity, String method, Map<String, Object> fields, Duration timeout) {

        byte[] body = writeJson(fields);

        HttpRequest request = requestFor(identity, method, timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return exchange(identity, method, request, fields);
    }

    /** Calls a method that carries bytes, as {@code multipart/form-data}. */
    protected JsonNode upload(
            TelegramIdentity identity, String method, MultipartBody body) {

        HttpRequest request = requestFor(identity, method, readTimeout)
                .header("Content-Type", body.contentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.content()))
                .build();

        return exchange(identity, method, request, Map.of());
    }

    private HttpRequest.Builder requestFor(TelegramIdentity identity, String method, Duration timeout) {
        requireCredential(identity);

        String address = "%s/bot%s/%s".formatted(
                trimTrailingSlash(identity.apiBaseOrDefault()), identity.credential(), method);

        try {
            return HttpRequest.newBuilder().uri(URI.create(address)).timeout(timeout);
        } catch (IllegalArgumentException exception) {
            // ⚠️ Thrown before any request is made, and its message quotes the whole URL — which
            // contains the bot token. Rebuilt here without it.
            throw new TelegramException(new TelegramRefusal.Unauthorized(
                    identity.name(),
                    "the configured API base is not a valid address: " + identity.apiBaseOrDefault()),
                    exception);
        }
    }

    private JsonNode exchange(
            TelegramIdentity identity, String method, HttpRequest request, Map<String, Object> fields) {

        LOGGER.debug("bot api: identity={}, method={}, fields={}",
                identity.name(), method, fields.keySet());

        HttpResponse<byte[]> response;

        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException exception) {
            throw new TelegramException(
                    new TelegramRefusal.TransportFailure(exception.getMessage()), exception);
        } catch (InterruptedException exception) {
            // Restoring the flag matters: a polling loop is shut down by interrupting it, and a
            // swallowed interrupt is a thread that will not stop.
            Thread.currentThread().interrupt();
            throw new TelegramException(
                    new TelegramRefusal.TransportFailure("the call was interrupted"), exception);
        }

        return readReply(identity, method, response.body(), fields);
    }

    private JsonNode readReply(
            TelegramIdentity identity, String method, byte[] body, Map<String, Object> fields) {

        JsonNode reply;

        try {
            reply = mapper.readTree(body);
        } catch (IOException exception) {
            // A non-JSON body means something between us and Telegram answered - a proxy, a captive
            // portal, a misconfigured local Bot API server. Retryable, because it usually is.
            throw new TelegramException(new TelegramRefusal.TransportFailure(
                    "the reply to %s was not JSON".formatted(method)), exception);
        }

        if (reply.path("ok").asBoolean(false)) {
            return reply.path("result");
        }

        throw new TelegramException(BotApiErrors.translate(
                reply.path("error_code").asInt(),
                reply.path("description").asText(null),
                reply.get("parameters"),
                String.valueOf(fields.getOrDefault("chat_id", identity.name()))));
    }

    private void requireCredential(TelegramIdentity identity) {
        if (!identity.hasCredential()) {
            throw new TelegramException(new TelegramRefusal.Unauthorized(
                    identity.name(), "the identity has no bot token"));
        }
    }

    private byte[] writeJson(Map<String, Object> fields) {
        try {
            return mapper.writeValueAsBytes(fields);
        } catch (IOException exception) {
            throw new IllegalArgumentException("a request field could not be serialised", exception);
        }
    }

    private static String trimTrailingSlash(String address) {
        if (address.endsWith("/")) {
            return address.substring(0, address.length() - 1);
        }

        return address;
    }

    /** Exposed for a subclass that needs to write a nested structure into a single field. */
    protected String writeValueAsString(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalArgumentException("a request field could not be serialised", exception);
        }
    }
}
