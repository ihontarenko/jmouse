package org.jmouse.ai.mcp.authorization;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Proof that the client redeeming an authorization code is the one that asked for it — PKCE (RFC 7636).
 *
 * <p>Needed because the code travels to a loopback port, and <strong>a port is not a secret</strong>:
 * any other process on the machine can in principle be listening on it first. The client keeps a random
 * verifier to itself and publishes only its hash, so a code intercepted on the way back cannot be spent
 * without the process that started the flow.
 *
 * <p>⚠️ Only {@code S256} is accepted. The {@code plain} method the specification also allows would mean
 * the proof travels in the same request as the thing it is meant to protect, which is not a weaker
 * version of this — it is none of it.
 *
 * <p>A protocol concern rather than an account one, which is why it is in this library at all: nothing
 * here knows who is authorizing, only that whoever comes back is whoever left.
 */
public final class ProofKeyPolicy {

    /** The one challenge method that means anything. */
    public static final String SUPPORTED_METHOD = "S256";

    private static final String  DIGEST_ALGORITHM   = "SHA-256";
    private static final int     MINIMUM_LENGTH     = 43;
    private static final int     MAXIMUM_LENGTH     = 128;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[A-Za-z0-9\\-._~]+");

    /**
     * Vets a challenge before a code is minted against it.
     *
     * <p>Before rather than after, so a client cannot lock itself out of a code it has already been
     * issued — at redemption there is nothing useful to say about a challenge that was never usable.
     */
    public void requireChallenge(String challenge, String method) {
        if (method == null || method.isBlank()) {
            throw new McpAuthorizationException(
                    "A challenge method is required. Supported: " + SUPPORTED_METHOD + ".");
        }

        if (!SUPPORTED_METHOD.equalsIgnoreCase(method)) {
            throw new McpAuthorizationException("Challenge method '" + method + "' is not supported. "
                                              + "Supported: " + SUPPORTED_METHOD + ".");
        }

        if (!isWellFormed(challenge)) {
            throw new McpAuthorizationException(
                    "The challenge must be " + MINIMUM_LENGTH + " to " + MAXIMUM_LENGTH + " characters "
                    + "of unreserved URL characters — the base64url SHA-256 digest of a verifier the "
                    + "client keeps to itself.");
        }
    }

    /**
     * Whether the verifier presented at redemption hashes to the challenge stored with the code.
     *
     * <p>⚠️ One refusal for both failures, deliberately: a malformed verifier and a wrong one are told
     * apart by nobody except somebody guessing, and telling them apart is exactly what makes guessing
     * cheaper. Compared with {@link MessageDigest#isEqual} for the same reason.
     */
    public void requireMatchingVerifier(String challenge, String verifier) {
        if (!isWellFormed(verifier) || !matches(challenge, verifier)) {
            throw new McpAuthorizationException(
                    "This authorization code was issued to a client that proved possession of a "
                    + "verifier. The one presented does not match, so the code is refused.");
        }
    }

    private boolean isWellFormed(String value) {
        return value != null
            && value.length() >= MINIMUM_LENGTH
            && value.length() <= MAXIMUM_LENGTH
            && ALLOWED_CHARACTERS.matcher(value).matches();
    }

    private boolean matches(String challenge, String verifier) {
        return MessageDigest.isEqual(
                digestOf(verifier).getBytes(StandardCharsets.US_ASCII),
                challenge.getBytes(StandardCharsets.US_ASCII));
    }

    private String digestOf(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(DIGEST_ALGORITHM + " is unavailable", unavailable);
        }
    }
}
