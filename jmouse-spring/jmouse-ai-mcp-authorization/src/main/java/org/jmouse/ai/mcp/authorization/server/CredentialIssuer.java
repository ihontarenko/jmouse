package org.jmouse.ai.mcp.authorization.server;

/**
 * Where this module stops and a product begins: minting.
 *
 * <p>Everything above this interface is the protocol — a challenge that must be S256, an address that
 * must be loopback, a code that must be spendable once — and every installation answers those the same
 * way. Minting is the opposite. It needs to know what an account is, which accounts may hold a protocol
 * credential at all, how long one lasts, and what stops it working anywhere else, and no two products
 * here agree on any of that.
 *
 * <p>⚠️ <strong>They disagree about confinement in particular, and deliberately.</strong> One signs its
 * protocol credential with a secret only it holds, so that "this works nowhere else" is a signature that
 * does not verify; the other issues an ordinary credential and refuses non-protocol surfaces with a
 * declared check. That is a real disagreement about security posture, and this interface exists so that
 * it never has to be resolved: the module receives a token back and never learns what was signed, by
 * what, or against whom.
 */
public interface CredentialIssuer {

    /**
     * Mints against an approval a person has just given.
     *
     * <p>Called on <strong>redemption</strong>, never on approval. An approval nobody came back for
     * therefore leaves no live token and no row nobody claimed, and whatever the product checks about
     * the holder is checked against the party that actually turned up.
     */
    IssuedCredential issue(ApprovedAuthorization approval);

    /**
     * Renews a credential a client already holds.
     *
     * @throws org.jmouse.ai.mcp.authorization.McpAuthorizationException when the token is unknown,
     *         expired or revoked — the module answers that with the RFC's {@code invalid_grant}, which
     *         is the spelling a client branches on to decide whether to authorize again
     */
    IssuedCredential renew(String refreshToken);

    /**
     * What a person approved, in the only two facts this module holds about it.
     *
     * @param subjectReference whoever it was approved for, in the product's own vocabulary —
     *                         <strong>never read here</strong>; one product puts an agent account's
     *                         identifier there and another a member's, and neither is this module's
     *                         business
     * @param clientName       what the client called itself — ⚠️ a claim, for a log line and a screen,
     *                         never an identity
     * @param clientId         which registration it came from. ⚠️ <strong>This is the half a product
     *                         should key an agent on</strong>, and its absence was a real bug: one
     *                         product looked an agent up BY NAME, so two clients that both failed to
     *                         name themselves — which every client does once a registry has forgotten
     *                         them — collapsed into one agent, sharing its permissions and its switch.
     *                         Unlike the name, this is issued by the registry rather than claimed by
     *                         the client
     */
    record ApprovedAuthorization(String subjectReference, String clientName, String clientId) {}

    /**
     * A credential, in the three parts the token response is built from.
     *
     * @param accessToken      what the client presents on every call
     * @param refreshToken     what it presents to get another access token
     * @param expiresInSeconds how long the access token is honoured, as the response has to state it
     */
    record IssuedCredential(String accessToken, String refreshToken, long expiresInSeconds) {}
}
