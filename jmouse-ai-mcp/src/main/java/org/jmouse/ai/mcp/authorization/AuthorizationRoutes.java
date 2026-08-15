package org.jmouse.ai.mcp.authorization;

/**
 * Every route the authorization flow needs, derived from the prefixes a product chose.
 *
 * <p>Two prefixes rather than one, because a product may — and one does — put the paths a
 * <strong>client</strong> calls somewhere other than the paths a <strong>person</strong> calls. The
 * protocol prefix carries registration, authorization and the token endpoint; the consent prefix carries
 * review, approval and the screen itself. An installation that wants them together passes the same
 * prefix twice, which is what {@link #under(String)} does.
 *
 * <p>⚠️ <strong>Every one of these paths is load-bearing in a way an ordinary route is not.</strong> A
 * client is never told where to authorize: it calls the protocol endpoint without a credential, reads
 * the {@code resource_metadata} address off the {@code WWW-Authenticate} header, fetches two well-known
 * documents, and only then knows these exist. A path that moves is a client that cannot connect, and it
 * fails with nothing useful in any log — which is why this type exists at all, rather than two products
 * each writing the same constants out by hand and drifting.
 *
 * <p>⚠️ <strong>The well-known documents sit at the site root, and that is a deployment consequence
 * rather than a preference.</strong> RFC 9728 and RFC 8414 dictate the location, so a reverse proxy that
 * forwards only {@code /api} answers discovery with the frontend's HTML. {@link #WELL_KNOWN_PATTERN} is
 * what has to be added to the proxy and to the dev server alike. Both products learned this the hard way.
 *
 * @param protocolPrefix where a client registers, authorizes and redeems, e.g. {@code /api/auth/mcp}
 * @param consentPrefix  where a person reviews and approves, e.g. {@code /api/agents/authorization}
 * @param consentRoute   the screen a person actually sees — ⚠️ <strong>a plain value</strong>, never
 *                       derived or validated here, because it is a route in somebody else's interface
 */
public record AuthorizationRoutes(String protocolPrefix, String consentPrefix, String consentRoute) {

    /** Where discovery starts: what this resource is, and who authorizes for it (RFC 9728). */
    public static final String PROTECTED_RESOURCE_METADATA = AuthorizationDocuments.PROTECTED_RESOURCE_METADATA;

    /** What the authorization server can do, and where each of its endpoints lives (RFC 8414). */
    public static final String AUTHORIZATION_SERVER_METADATA = AuthorizationDocuments.AUTHORIZATION_SERVER_METADATA;

    /**
     * Both documents are also served with a resource path appended, because that is how a client asks
     * about one resource among several on a host. An installation protecting exactly one accepts the
     * suffix and ignores it rather than matching on it — refusing there strands a client over a spelling.
     */
    public static final String ANY_RESOURCE_SUFFIX = "/**";

    /** Everything discovery may ask for, for the security rule and the proxy rule alike. */
    public static final String WELL_KNOWN_PATTERN = "/.well-known/**";

    /**
     * The single scope a protocol credential carries. It grants nothing of its own — what a client may
     * do is decided by whoever approved it, never by what it asked for — but a scope has to be named for
     * the documents to be well-formed.
     */
    public static final String SCOPE = "mcp";

    public AuthorizationRoutes {
        protocolPrefix = withoutTrailingSlash(protocolPrefix);
        consentPrefix  = withoutTrailingSlash(consentPrefix);
    }

    /** The common shape: a client and a person walk paths under one prefix. */
    public static AuthorizationRoutes under(String prefix, String consentRoute) {
        return new AuthorizationRoutes(prefix, prefix, consentRoute);
    }

    /** A client announces itself and gets an identifier back (RFC 7591). */
    public String registration() {
        return protocolPrefix + "/register";
    }

    /** Where a client sends its person to approve it; hands over to the consent screen. */
    public String authorization() {
        return protocolPrefix + "/authorize";
    }

    /** Where a code becomes a credential, and a credential is later renewed. */
    public String token() {
        return protocolPrefix + "/token";
    }

    /** What the consent screen shows, so nothing is approved that cannot then be honoured. */
    public String review() {
        return consentPrefix + "/review";
    }

    /** The approval itself: the one place a live authorization code is minted. */
    public String approval() {
        return consentPrefix + "/approve";
    }

    /** Everything a client may call, for the one security rule that opens them all. */
    public String protocolPattern() {
        return protocolPrefix + "/**";
    }

    /** Everything a person may call, for the rule that requires them to be signed in. */
    public String consentPattern() {
        return consentPrefix + "/**";
    }

    private static String withoutTrailingSlash(String prefix) {
        if (prefix != null && prefix.endsWith("/")) {
            return prefix.substring(0, prefix.length() - 1);
        }

        return prefix;
    }
}
