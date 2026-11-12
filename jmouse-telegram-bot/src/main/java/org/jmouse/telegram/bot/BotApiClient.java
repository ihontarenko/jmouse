package org.jmouse.telegram.bot;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpCallException;
import org.jmouse.http.client.HttpReply;
import org.jmouse.http.client.HttpTransport;
import org.jmouse.http.client.JdkHttpTransport;
import tools.jackson.databind.JsonNode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
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

    /** The largest file Telegram serves a bot: 20 MB. */
    public static final long LARGEST_FILE = 20L * 1024 * 1024;

    /** How much of a method's reply is read at most: replies are small, a page of updates the largest. */
    private static final long LARGEST_REPLY = 4L * 1024 * 1024;

    private final HttpTransport transport;
    private final Duration      readTimeout;

    protected BotApiClient() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected BotApiClient(Duration connectTimeout, Duration readTimeout) {
        // Never a redirect: the address carries the bot's token, which must not be carried anywhere else.
        this.transport   = new JdkHttpTransport(connectTimeout, JdkHttpTransport.Redirects.NEVER);
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

        HttpCall call = callTo(identity, addressOf(identity, "bot%s/" + method), timeout, LARGEST_REPLY)
                .body(writeJson(fields), "application/json; charset=utf-8").build();

        return exchange(identity, method, call, fields);
    }

    /** Calls a method that carries bytes, as {@code multipart/form-data}. */
    protected JsonNode upload(
            TelegramIdentity identity, String method, MultipartBody body) {

        HttpCall call = callTo(identity, addressOf(identity, "bot%s/" + method), readTimeout, LARGEST_REPLY)
                .body(body.content(), body.contentType()).build();

        return exchange(identity, method, call, Map.of());
    }

    /**
     * Opens a file Telegram is holding, at {@code <api base>/file/bot<token>/<path>}, read whole — Telegram serves a
     * bot no file larger than {@link #LARGEST_FILE} — and handed over as a stream.
     *
     * <p>⚠️ Not {@code exchange}: the answer is the file, not JSON, and a failure is an HTTP status with no
     * Telegram description to translate. And the address carries the token, so nothing here ever puts it
     * into a message.
     */
    protected InputStream download(TelegramIdentity identity, String path) {
        HttpCall call = HttpCall.get(fileAddressOf(identity, path)).timeout(readTimeout).responseLimit(LARGEST_FILE).build();
        HttpReply reply;
        try {
            reply = transport.exchange(call);
        } catch (HttpCallException exception) {
            throw new TelegramException(new TelegramRefusal.TransportFailure(downloadFailure(exception)), exception);
        }
        if (reply.status() != 200) {
            throw new TelegramException(new TelegramRefusal.TransportFailure(
                    "the file could not be downloaded (HTTP %d)".formatted(reply.status())));
        }
        return new ByteArrayInputStream(reply.body());
    }

    private static String downloadFailure(HttpCallException exception) {
        return switch (exception.failure()) {
            case INTERRUPTED -> "the download was interrupted";
            case RESPONSE_LIMIT -> "the file is larger than Telegram serves a bot";
            case TIMEOUT, TRANSPORT -> exception.getMessage();
        };
    }

    /** A file's address; one that is not a valid address is said without the token it carries. */
    private URI fileAddressOf(TelegramIdentity identity, String path) {
        requireCredential(identity);
        try {
            return URI.create("%s/file/bot%s/%s".formatted(trimTrailingSlash(identity.apiBaseOrDefault()), identity.credential(), path));
        } catch (IllegalArgumentException exception) {
            throw new TelegramException(new TelegramRefusal.TransportFailure("the file path is not a valid address"), exception);
        }
    }

    /** A method's address: {@code <api base>/bot<token>/<method>}, the token put in where {@code pattern} says. */
    private URI addressOf(TelegramIdentity identity, String pattern) {
        requireCredential(identity);
        try {
            return URI.create(trimTrailingSlash(identity.apiBaseOrDefault()) + "/" + pattern.formatted(identity.credential()));
        } catch (IllegalArgumentException exception) {
            // ⚠️ Thrown before any request is made, and its message quotes the whole URL — which
            // contains the bot token. Rebuilt here without it.
            throw new TelegramException(new TelegramRefusal.Unauthorized(
                    identity.name(), "the configured API base is not a valid address: " + identity.apiBaseOrDefault()),
                    exception);
        }
    }

    private static HttpCall.Builder callTo(TelegramIdentity identity, URI address, Duration timeout, long limit) {
        try {
            return HttpCall.post(address).timeout(timeout).responseLimit(limit);
        } catch (IllegalArgumentException exception) {
            throw new TelegramException(new TelegramRefusal.Unauthorized(
                    identity.name(), "the configured API base is not an HTTP address: " + identity.apiBaseOrDefault()),
                    exception);
        }
    }

    private JsonNode exchange(TelegramIdentity identity, String method, HttpCall call, Map<String, Object> fields) {
        LOGGER.debug("bot api: identity={}, method={}, fields={}", identity.name(), method, fields.keySet());
        try {
            return readReply(identity, method, transport.exchange(call).body(), fields);
        } catch (HttpCallException exception) {
            // The client's sentence names the host alone, never the address: the address carries the bot's token.
            // An interrupted call keeps the thread's flag — a polling loop is shut down by interrupting it.
            String detail = exception.failure() == HttpCallException.Failure.INTERRUPTED ? "the call was interrupted"
                    : exception.getMessage();
            throw new TelegramException(new TelegramRefusal.TransportFailure(detail), exception);
        }
    }

    private JsonNode readReply(
            TelegramIdentity identity, String method, byte[] body, Map<String, Object> fields) {

        JsonNode reply;

        try {
            reply = mapper.readTree(body);
        } catch (JacksonException exception) {
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
                reply.path("description").asString(null),
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
        } catch (JacksonException exception) {
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
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("a request field could not be serialised", exception);
        }
    }
}
