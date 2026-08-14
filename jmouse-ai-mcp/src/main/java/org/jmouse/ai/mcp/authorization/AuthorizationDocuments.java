package org.jmouse.ai.mcp.authorization;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The two documents that make a protocol endpoint connectable.
 *
 * <p>A client reads them <strong>before it holds any credential</strong> — that is the entire reason
 * they exist — so both are necessarily public, and everything they disclose is a route that was already
 * reachable. RFC 9728 for the first, RFC 8414 for the second.
 *
 * <p>Built here rather than written out by each product because the field names are a specification's
 * and every one of them is load-bearing: a client that cannot find {@code authorization_servers} does
 * not fall back to asking, it fails to connect.
 *
 * <p>⚠️ <strong>Serving them is the product's.</strong> This library has no HTTP layer and should not
 * grow one; what it owns is the shape. A product serves each at its well-known location and — worth
 * copying — accepts a resource path appended to that location rather than matching on it. A client
 * asking about one resource on a host appends that resource's path, and refusing on a suffix mismatch
 * strands clients over a spelling.
 */
public final class AuthorizationDocuments {

    /** Where a client looks for the first document, before the resource path it may append. */
    public static final String PROTECTED_RESOURCE_METADATA = "/.well-known/oauth-protected-resource";

    /** And the second. */
    public static final String AUTHORIZATION_SERVER_METADATA = "/.well-known/oauth-authorization-server";

    private static final String BEARER_IN_HEADER = "header";

    private AuthorizationDocuments() {
    }

    /**
     * What this endpoint is, and who issues credentials for it.
     *
     * @param resourceUrl        the absolute address the protocol is served at
     * @param authorizationServer the absolute issuer a client should go to for a credential
     * @param resourceName       what to call this installation on an approval screen
     * @param scope              the one scope a protocol credential carries
     */
    public static Map<String, Object> protectedResource(
            String resourceUrl, String authorizationServer, String resourceName, String scope) {

        Map<String, Object> document = new LinkedHashMap<>();

        document.put("resource",                 resourceUrl);
        document.put("authorization_servers",    List.of(authorizationServer));
        document.put("scopes_supported",         List.of(scope));
        document.put("bearer_methods_supported", List.of(BEARER_IN_HEADER));
        document.put("resource_name",            resourceName);

        return document;
    }

    /**
     * Where a client authorizes, redeems a code, and registers itself.
     *
     * <p>⚠️ States {@code S256} as the only challenge method and {@code none} as the only client
     * authentication method, and those two go together: a client on somebody's laptop cannot keep a
     * secret, so it proves possession instead. A document offering {@code plain} beside {@code S256}
     * would have well-behaved clients choose the weaker one on a coin toss.
     */
    public static Map<String, Object> authorizationServer(
            String issuer,
            String authorizationEndpoint,
            String tokenEndpoint,
            String registrationEndpoint,
            String scope) {

        Map<String, Object> document = new LinkedHashMap<>();

        document.put("issuer",                                issuer);
        document.put("authorization_endpoint",                authorizationEndpoint);
        document.put("token_endpoint",                        tokenEndpoint);
        document.put("registration_endpoint",                 registrationEndpoint);
        document.put("response_types_supported",              List.of("code"));
        document.put("grant_types_supported",                 List.of("authorization_code", "refresh_token"));
        document.put("code_challenge_methods_supported",      List.of(ProofKeyPolicy.SUPPORTED_METHOD));
        document.put("token_endpoint_auth_methods_supported", List.of("none"));
        document.put("scopes_supported",                      List.of(scope));

        return document;
    }
}
