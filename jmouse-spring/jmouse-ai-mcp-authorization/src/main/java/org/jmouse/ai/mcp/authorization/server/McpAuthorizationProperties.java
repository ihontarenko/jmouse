package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationRoutes;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * The handful of facts the shared flow is built out of, stated by whoever is hosting it.
 *
 * <p>⚠️ <strong>Two base addresses, because in development they are two servers.</strong>
 * {@link #resourceUrl} is where a <em>client</em> reaches the API — the address the discovery documents
 * publish. {@link #browserUrl} is where a <em>person</em> reaches the product, which locally is a
 * development server on another port and once deployed is the same origin. Building the consent redirect
 * from the API address sends somebody's browser to a backend that serves no interface; building the
 * discovery documents from the browser address publishes an origin the protocol is not served on.
 * Neither failure is visible from the other side.
 *
 * <p>⚠️ <strong>Two prefixes, because one product splits them.</strong> See {@link AuthorizationRoutes}.
 * The defaults put everything under one, which is the common shape.
 */
@ConfigurationProperties(prefix = McpAuthorizationProperties.PREFIX)
public class McpAuthorizationProperties {

    public static final String PREFIX = "jmouse.mcp.authorization";

    /** Where a client registers, authorizes and redeems, when nothing says otherwise. */
    public static final String DEFAULT_PROTOCOL_PREFIX = "/api/agents/authorization";

    /**
     * The prefix a request mapping is built from, as a placeholder rather than a value.
     *
     * <p>A path is decided by whoever is hosting this and has to be known before any bean exists, which
     * is what a placeholder in a mapping is for. Spelled once here so that the mapping and
     * {@link #routes()} cannot drift into answering two different paths — which would be a client sent
     * somewhere nothing is served.
     */
    public static final String PROTOCOL_PREFIX_EXPRESSION =
            "${" + PREFIX + ".protocol-prefix:" + DEFAULT_PROTOCOL_PREFIX + "}";

    /** The same, for the paths a person calls — defaulting to wherever a client's paths are. */
    public static final String CONSENT_PREFIX_EXPRESSION =
            "${" + PREFIX + ".consent-prefix:" + PROTOCOL_PREFIX_EXPRESSION + "}";

    /** What the consent screen calls this installation — the product's own display name. */
    private String applicationName = "";

    /** Where a client reaches this API, absolute, without a trailing slash. */
    private String resourceUrl = "";

    /** Where a person reaches this product's interface, absolute, without a trailing slash. */
    private String browserUrl = "";

    private String protocolPrefix = DEFAULT_PROTOCOL_PREFIX;

    /** Empty means "wherever a client's paths are", which is the common shape. */
    private String consentPrefix = "";

    /** Long enough for a browser round trip and a client's callback; short enough to be worth stealing. */
    private Duration codeLifetime = Duration.ofSeconds(120);

    /**
     * How long a client's registered name is remembered.
     *
     * <p>⚠️ Longer than it sounds it needs to be, because the name is not only shown on the consent
     * screen. It is copied onto the connection row at approval, and in some products it becomes the
     * agent's name; a client re-authorising after its registration lapsed comes back as
     * <em>An unnamed client</em>, and that string then sticks. A month covers the whole life of a
     * refresh credential, which is the window in which a client will plausibly come back.
     *
     * <p>It expires at all because a registration nobody has used since is a row about a client that
     * is gone — and because the identifier confers nothing, so keeping one forever buys nothing either.
     */
    private Duration clientRegistrationLifetime = Duration.ofDays(30);

    /**
     * What an issued access token's {@code aud} claim says, and therefore which decoder will take it.
     *
     * <p>⚠️ <strong>Left empty on purpose, and {@link AgentCredentials} refuses to start without it.</strong>
     * A default here would be a default audience — a token this product mints being accepted by whichever
     * service happens to share the guess. The one value that cannot be inferred is the one that must be
     * stated.
     */
    private String audience = "";

    /**
     * How long a minted access token stands.
     *
     * <p>Short by design: it is self-contained, so nothing consults a row while it is valid and a
     * revocation cannot reach one that is already out. The refresh token is what makes a long-lived
     * connection possible without a long-lived credential.
     */
    private Duration accessTokenLifetime = Duration.ofHours(1);

    /**
     * How long a connection may go unused before its refresh token stops working.
     *
     * <p>⚠️ The window <em>slides</em> on every renewal — a fixed one would end a connection somebody uses
     * daily, on a date nobody chose.
     */
    private Duration refreshTokenLifetime = Duration.ofDays(30);

    /**
     * The exact paths a client may be sent back to on its own machine.
     *
     * <p>⚠️ Exact paths rather than a pattern: this list is the last thing between a live authorization
     * code and an open redirect. Claude Code listens on {@code /callback}; the other two are what the
     * clients that do not are observed to use.
     */
    private List<String> allowedRedirectPaths = List.of("/callback", "/oauth/callback", "/auth/callback");

    /** The loopback names a client may be returned to. Narrower is safer; see the redirect policy. */
    private List<String> allowedRedirectHosts = List.of("127.0.0.1", "localhost", "[::1]");

    /**
     * Whether a client may also be sent to its own segments under one of those paths.
     *
     * <p>Off, so that an installation that has not thought about it keeps the strictest rule. Turn it on
     * for a client that puts a fresh nonce in the path it listens on — Codex registers
     * {@code /callback/<nonce>} and there is therefore no spelling of it to add to the list above. The
     * segments are held to unreserved characters and cannot climb out of the path they hang under; see
     * {@link org.jmouse.ai.mcp.authorization.LoopbackRedirectPolicy} for why that gives nothing away.
     */
    private boolean allowNestedRedirectPaths = false;

    private final Consent consent = new Consent();

    /** Every route the flow needs, derived once from the prefixes above. */
    public AuthorizationRoutes routes() {
        String consentUnder = consentPrefix == null || consentPrefix.isBlank() ? protocolPrefix : consentPrefix;

        return new AuthorizationRoutes(protocolPrefix, consentUnder, consentUnder + Consent.PATH);
    }

    /** An absolute API address for a path a client is told to call. */
    public String apiUrl(String path) {
        return withoutTrailingSlash(resourceUrl) + path;
    }

    /** An absolute address for a page a person's browser is sent to. */
    public String browserUrl(String path) {
        return withoutTrailingSlash(browserUrl) + path;
    }

    /**
     * How the shared consent screen finds the person who is already signed in.
     *
     * <p>⚠️ <strong>The screen is served by this library but runs on the product's own origin</strong> —
     * a person reaches it through the same proxy that serves the interface — so it can read the token
     * the product's own application put in web storage, which is the only thing that lets one page work
     * for two products whose sign-in mechanisms have nothing in common. What differs is the key and the
     * shape, and that is exactly what these two values say.
     */
    public static class Consent {

        /** Appended to the consent prefix. The page is public: it grants nothing and shows nothing yet. */
        public static final String PATH = "/consent";

        /** The web-storage key the product's interface keeps its access token under. */
        private String tokenStorageKey = "";

        /** The field to read out of that entry when it is JSON; empty when the value is the token. */
        private String tokenStorageField = "";

        /** Where to send somebody who turns out not to be signed in at all. */
        private String signInRoute = "/";

        /**
         * Which consent design this installation serves.
         *
         * <p>The name of a template beside {@code consent-frame.j.html}, without the suffix. Every one of
         * them extends that frame, so the flow — who is signed in, what the server allows, what happens to
         * a one-time code — is the same page whichever is chosen; only the markup and the stylesheet differ.
         *
         * <p>⚠️ <strong>A name nothing matches fails at startup, not at first use.</strong> The page is
         * rendered once when {@link ConsentPage} is constructed, so a typo here is a boot failure rather
         * than a blank screen the first time somebody connects a client — which is the whole reason it is
         * rendered eagerly.
         */
        private String template = "consent-aurora";

        public String getTokenStorageKey() {
            return tokenStorageKey;
        }

        public void setTokenStorageKey(String tokenStorageKey) {
            this.tokenStorageKey = tokenStorageKey;
        }

        public String getTokenStorageField() {
            return tokenStorageField;
        }

        public void setTokenStorageField(String tokenStorageField) {
            this.tokenStorageField = tokenStorageField;
        }

        public String getSignInRoute() {
            return signInRoute;
        }

        public void setSignInRoute(String signInRoute) {
            this.signInRoute = signInRoute;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }
    }

    private static String withoutTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getBrowserUrl() {
        return browserUrl;
    }

    public void setBrowserUrl(String browserUrl) {
        this.browserUrl = browserUrl;
    }

    public String getProtocolPrefix() {
        return protocolPrefix;
    }

    public void setProtocolPrefix(String protocolPrefix) {
        this.protocolPrefix = protocolPrefix;
    }

    public String getConsentPrefix() {
        return consentPrefix;
    }

    public void setConsentPrefix(String consentPrefix) {
        this.consentPrefix = consentPrefix;
    }

    public Duration getCodeLifetime() {
        return codeLifetime;
    }

    public Duration getClientRegistrationLifetime() {
        return clientRegistrationLifetime;
    }

    public void setClientRegistrationLifetime(Duration clientRegistrationLifetime) {
        this.clientRegistrationLifetime = clientRegistrationLifetime;
    }

    public void setCodeLifetime(Duration codeLifetime) {
        this.codeLifetime = codeLifetime;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getAccessTokenLifetime() {
        return accessTokenLifetime;
    }

    public void setAccessTokenLifetime(Duration accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public Duration getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    public void setRefreshTokenLifetime(Duration refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    public List<String> getAllowedRedirectPaths() {
        return allowedRedirectPaths;
    }

    public void setAllowedRedirectPaths(List<String> allowedRedirectPaths) {
        this.allowedRedirectPaths = allowedRedirectPaths;
    }

    public List<String> getAllowedRedirectHosts() {
        return allowedRedirectHosts;
    }

    public void setAllowedRedirectHosts(List<String> allowedRedirectHosts) {
        this.allowedRedirectHosts = allowedRedirectHosts;
    }

    public boolean isAllowNestedRedirectPaths() {
        return allowNestedRedirectPaths;
    }

    public void setAllowNestedRedirectPaths(boolean allowNestedRedirectPaths) {
        this.allowNestedRedirectPaths = allowNestedRedirectPaths;
    }

    public Consent getConsent() {
        return consent;
    }
}
