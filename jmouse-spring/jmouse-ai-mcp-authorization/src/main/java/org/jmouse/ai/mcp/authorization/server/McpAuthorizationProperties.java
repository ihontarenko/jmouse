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

    /** What the consent screen calls this installation: "Innoventa", "Tessera". */
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
     * The exact paths a client may be sent back to on its own machine.
     *
     * <p>⚠️ Exact paths rather than a pattern: this list is the last thing between a live authorization
     * code and an open redirect. Claude Code listens on {@code /callback}; the other two are what the
     * clients that do not are observed to use.
     */
    private List<String> allowedRedirectPaths = List.of("/callback", "/oauth/callback", "/auth/callback");

    /** The loopback names a client may be returned to. Narrower is safer; see the redirect policy. */
    private List<String> allowedRedirectHosts = List.of("127.0.0.1", "localhost", "[::1]");

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

    public void setCodeLifetime(Duration codeLifetime) {
        this.codeLifetime = codeLifetime;
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

    public Consent getConsent() {
        return consent;
    }
}
