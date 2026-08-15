package org.jmouse.ai.mcp.authorization;

/**
 * The specification's own words, spelled once for everybody.
 *
 * <p>Every constant here is a value a <strong>program</strong> compares literally: a client branches on
 * {@code error} to decide between "authorize again" and "give up", and reads {@code token_type} to
 * decide how to present what it was handed. A typo in any of them is not a message somebody reads and
 * corrects — it is a client silently taking the wrong branch, at connection time, with nothing useful in
 * any log. That is the whole argument for one home rather than one per product.
 *
 * <p>⚠️ <strong>The S256 spelling is deliberately not here.</strong> It already exists as
 * {@link ProofKeyPolicy#SUPPORTED_METHOD}, beside the code that enforces it, and a second spelling of it
 * is exactly the kind of drift this class exists to prevent — the promise in a discovery document and
 * the check at the endpoint have to be the same characters or a client is told one thing and refused for
 * another.
 *
 * <p>RFC 6749 for the grants and the refusals, RFC 7591 for {@link #ERROR_INVALID_REDIRECT_URI}, RFC 6750
 * for {@link #BEARER_TOKEN_TYPE}.
 */
public final class AuthorizationVocabulary {

    /** Spending a one-time authorization code. */
    public static final String AUTHORIZATION_CODE = "authorization_code";

    /** Renewing a credential that has run out. */
    public static final String REFRESH_TOKEN = "refresh_token";

    /** The only response type: the person's browser comes back carrying a code. */
    public static final String RESPONSE_TYPE_CODE = "code";

    /** Clients here are public — they run on somebody's own machine and can keep no secret. */
    public static final String NO_CLIENT_AUTHENTICATION = "none";

    /** What a bearer credential is called in the response that hands it over. */
    public static final String BEARER_TOKEN_TYPE = "Bearer";

    // ── Refusal codes (RFC 6749 §4.1.2.1, §5.2) ──────────────────────────────────
    // A client branches on these, so they are the specification's spellings and not anybody's.

    /** The request was malformed or missing something required. */
    public static final String ERROR_INVALID_REQUEST = "invalid_request";

    /** The code or refresh token was unknown, expired, already spent, or unproven. */
    public static final String ERROR_INVALID_GRANT = "invalid_grant";

    /** A grant type this server does not implement. */
    public static final String ERROR_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

    /** A response type this server does not implement. */
    public static final String ERROR_UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";

    /** The server failed for a reason that is not the client's doing. */
    public static final String ERROR_SERVER_ERROR = "server_error";

    /** Registration named an address this server will not send a live code to (RFC 7591 §3.2.2). */
    public static final String ERROR_INVALID_REDIRECT_URI = "invalid_redirect_uri";

    private AuthorizationVocabulary() {
    }
}
