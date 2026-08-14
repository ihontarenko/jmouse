package org.jmouse.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Everything that is the same about talking to any of them over HTTP.
 *
 * <p>A model implementation is then four small methods — where to send it, what to put in the headers,
 * what the body looks like and how to read the answer — and none of them knows about timeouts, JSON,
 * status codes or what an unreachable host looks like. That split is what makes a third provider a
 * afternoon's work rather than a fourth copy of this file.
 *
 * <p>⚠️ <strong>{@link #converse} is final.</strong> The checks it runs before the call — settings
 * exist, they are addressed to this model, there is a credential, there is an address — are the ones
 * whose absence produces the most confusing possible failures: a right key against a wrong endpoint,
 * or a {@code NullPointerException} an hour into a conversation.
 *
 * <h2>Timeouts</h2>
 *
 * <p>Both finite, and here rather than left to the caller. A hung provider otherwise blocks the calling
 * thread forever, and enough of those exhaust a server's whole pool — the failure then looks like the
 * application being down rather than one integration being slow. Generous rather than typical: a
 * provider can legitimately take a while to finish a response with several tool calls in it, and a read
 * timeout tuned to an ordinary API is a timeout that fires on the interesting requests only.
 *
 * <h2>The three ways this fails</h2>
 *
 * <p>All three become a {@link ProviderException} carrying a sentence somebody can read, and they are
 * translated separately because they have three different fixes: the provider said no (a bad key, a
 * model that does not exist, a body it did not like), nothing answered at all (refused, timed out, DNS),
 * or the address was never an address — which arrives, unhelpfully, as an
 * {@link IllegalArgumentException} thrown before any request is made.
 */
public abstract class HttpChatModel implements ChatModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpChatModel.class);

    /** Long enough for a slow handshake, short enough that a black hole is noticed. */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /** Long enough for a multi-tool-call answer to finish being written. */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

    private final ProviderSettingsSource settingsSource;
    private final HttpClient             httpClient;
    private final Duration               readTimeout;

    protected HttpChatModel(ProviderSettingsSource settingsSource) {
        this(settingsSource, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    protected HttpChatModel(
            ProviderSettingsSource settingsSource, Duration connectTimeout, Duration readTimeout) {

        this.settingsSource = settingsSource;
        this.readTimeout    = readTimeout;
        this.httpClient     = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    @Override
    public final ChatResponse converse(ChatRequest request) {
        ProviderSettings settings = resolveSettings();
        String           url      = resolveUrl(settings);

        LOGGER.debug("{}: model={}, messages={}, tools={}",
                providerName(), settings.model(), request.messages().size(), request.tools().size());

        return parseResponse(post(url, settings, requestBody(request, settings)));
    }

    // ── What an implementation supplies ──────────────────────────────────────────

    /** Where this provider lives, for settings that do not say. Null for one that has no fixed home. */
    protected abstract String defaultApiUrl();

    /**
     * Authentication, and anything else this provider insists on.
     *
     * <p>Content type and accept are not an implementation's business — they are the same for all of
     * them and are set here, which is one fewer thing three files can disagree about.
     */
    protected abstract Map<String, String> headers(ProviderSettings settings);

    protected abstract Map<String, Object> requestBody(ChatRequest request, ProviderSettings settings);

    protected abstract ChatResponse parseResponse(Map<String, Object> answer);

    /**
     * Whether this provider will not talk without a credential.
     *
     * <p>True for anything on the public internet. A gateway inside a trust boundary is the exception
     * and says so for itself.
     */
    protected boolean requiresApiKey() {
        return true;
    }

    // ── Before the call ──────────────────────────────────────────────────────────

    private ProviderSettings resolveSettings() {
        ProviderSettings settings = settingsSource.settings();

        if (settings == null) {
            throw new ProviderException("No AI provider settings are configured, so there is nothing to "
                                      + "call. Configure a provider, a model and a key.");
        }

        // ⚠️ Before anything is sent. Settings addressed elsewhere otherwise send one provider's key to
        // another provider's endpoint, and the failure that comes back reads as a bad credential.
        if (settings.providerName() != null && !settings.providerName().equals(providerName())) {
            throw new ProviderException(
                    "These settings are for '" + settings.providerName() + "' and this is the '"
                    + providerName() + "' model. Whatever chooses between them is picking the wrong one.");
        }

        if (requiresApiKey() && !settings.hasApiKey()) {
            throw new ProviderException(
                    "The '" + providerName() + "' provider has no API key configured, so the call cannot "
                    + "be authenticated. Set one and try again — nothing was sent.");
        }

        return settings;
    }

    private String resolveUrl(ProviderSettings settings) {
        String url = settings.apiUrlOr(defaultApiUrl());

        if (url == null || url.isBlank()) {
            throw new ProviderException(
                    "The '" + providerName() + "' provider has no address configured and no default of "
                    + "its own. Set the API url in its settings.");
        }

        return url;
    }

    // ── The call, and the three ways it fails ────────────────────────────────────

    private Map<String, Object> post(String url, ProviderSettings settings, Map<String, Object> body) {
        HttpRequest httpRequest = buildRequest(url, settings, body);
        HttpResponse<String> response;

        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        } catch (IOException noAnswer) {
            // Connection refused, a timeout, a name that does not resolve. Nothing arrived, so there is
            // no body to explain it and the exception's own message is the best there is.
            LOGGER.warn("{} did not answer at {}: {}", providerName(), url, noAnswer.getMessage());

            throw new ProviderException(providerName() + " is unreachable at " + url + ": "
                                      + describe(noAnswer) + ". Nothing was sent that it acted on.",
                                        noAnswer);

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();

            throw new ProviderException(providerName() + " was still answering when this thread was "
                                      + "interrupted. Whether it finished is not knowable from here.",
                                        interrupted);
        }

        return readAnswer(response);
    }

    private HttpRequest buildRequest(String url, ProviderSettings settings, Map<String, Object> body) {
        HttpRequest.Builder request;

        try {
            request = HttpRequest.newBuilder(URI.create(url));

        } catch (IllegalArgumentException notAnAddress) {
            // ⚠️ Thrown synchronously, before anything is sent, and so easy to let escape as an
            // unexplained IllegalArgumentException a long way from the setting that caused it.
            throw new ProviderException(
                    "'" + url + "' is not a usable address for " + providerName() + ": "
                    + notAnAddress.getMessage() + ". It needs a scheme and a host, e.g. "
                    + "'https://api.example.com/v1/messages'.", notAnAddress);
        }

        headers(settings).forEach(request::header);

        return request
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .timeout(readTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();
    }

    private Map<String, Object> readAnswer(HttpResponse<String> response) {
        if (response.statusCode() >= 400) {
            LOGGER.warn("{} refused with {}: {}",
                    providerName(), response.statusCode(), Json.truncate(response.body()));

            throw new ProviderException(providerName() + " request failed (" + response.statusCode()
                                      + "): " + describeProviderError(response.body()));
        }

        if (response.body() == null || response.body().isBlank()) {
            throw new ProviderException(providerName() + " answered " + response.statusCode()
                                      + " with an empty body, so there is nothing to read.");
        }

        return Json.readMap(response.body(), providerName());
    }

    /**
     * The sentence inside the wall of JSON.
     *
     * <p>Both major providers shape an error as {@code {"error": {"message": …}}}, and pulling that
     * string out is the whole difference between something a user can act on and something they
     * forward to somebody else. Falls back to a truncated body rather than to nothing, because an HTML
     * page from a proxy still says which proxy.
     */
    private String describeProviderError(String body) {
        return Json.tryReadMap(body)
                .filter(parsed -> parsed.get("error") instanceof Map<?, ?>)
                .map(parsed -> (Map<?, ?>) parsed.get("error"))
                .filter(error -> error.get("message") instanceof String)
                .map(error -> (String) error.get("message"))
                .orElseGet(() -> Json.truncate(body));
    }

    /** An exception whose message is sometimes only a hostname, which on its own explains nothing. */
    private String describe(Exception failure) {
        return failure.getMessage() == null
                ? failure.getClass().getSimpleName()
                : failure.getClass().getSimpleName() + " (" + failure.getMessage() + ")";
    }
}
