package org.jmouse.telegram.jpa;

/**
 * Seals a credential before it is written, and opens it when it is read.
 *
 * <h2>⚠️ Why a column cannot simply hold the token</h2>
 *
 * <p>A bot token is complete control of the bot: read it and you can send as it, read its updates, and
 * change what it is. A {@link org.jmouse.telegram.IdentityKind#USER} session, when that arrives, is
 * complete control of a <em>person's account</em>.
 *
 * <p>A plaintext column puts that in every database backup, every replica, every dump somebody takes to
 * debug something, and in the output of any query an operator runs. None of those are places a rotation
 * can reach afterwards — which is what makes this the one decision that cannot be fixed later.
 *
 * <h2>⚠️ There is deliberately no plaintext implementation</h2>
 *
 * <p>A pass-through default would be the state an installation reaches by <em>forgetting</em> to
 * configure a key, which is the worst possible way to arrive at storing tokens in the clear. Same
 * reasoning as the webhook secret: refuse, loudly, naming what to set.
 *
 * <p>A product that genuinely wants plaintext — a scratch installation, a smoke run — writes the
 * two-line implementation itself. That is a decision somebody has to type out, which is the point.
 *
 * <h2>The shape</h2>
 *
 * <p>Both directions take and return a {@link String}, because what goes in the column is text and the
 * caller should not care whether that is base64, a KMS reference, or a Vault path. An implementation
 * over a key-management service is a legitimate implementation and no caller changes.
 */
public interface CredentialCipher {

    /**
     * @param credential the token or session, in the clear
     * @return what to store; ⚠️ must be safe to place in a text column, a backup and a log
     */
    String seal(String credential);

    /**
     * @param sealed exactly what {@link #seal} produced
     * @return the credential in the clear
     * @throws IllegalStateException when the value cannot be opened — a rotated key, a truncated
     *                               column, a value written by a different implementation. ⚠️ Never
     *                               returns null or the sealed text on failure: a caller would send
     *                               ciphertext to Telegram as a token and read the refusal as a
     *                               revoked credential
     */
    String open(String sealed);
}
