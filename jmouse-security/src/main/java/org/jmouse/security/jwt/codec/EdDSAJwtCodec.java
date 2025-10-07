package org.jmouse.security.jwt.codec;

import org.jmouse.security.jwt.AbstractJwtCodec;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;

/**
 * 🪶 EdDSAJwtCodec — Ed25519 codec.
 *
 * <p>Provides JOSE-compliant signature handling for EdDSA (Ed25519):</p>
 * <ul>
 *   <li>✍️ {@link #sign(byte[])} — sign JWT content with Ed25519 private key</li>
 *   <li>✅ {@link #verify(byte[], byte[])} — verify JWT signature using Ed25519 public key</li>
 * </ul>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>🔑 If {@code privateKey} is {@code null}, the codec works in verify-only mode.</li>
 *   <li>📏 Ed25519 signatures are always 64 bytes (r||s raw format, no DER conversion).</li>
 *   <li>⏱️ Clock and skew can be customized for token validation.</li>
 * </ul>
 */
public final class EdDSAJwtCodec extends AbstractJwtCodec {

    /**
     * 🔑 JCA algorithm name for Ed25519.
     */
    public static final String ED_25519 = "Ed25519";

    private final PublicKey  publicKey;
    private final PrivateKey privateKey; // null => decode-only

    /**
     * 🏗️ Construct EdDSA codec with default clock/skew.
     *
     * @param json       JSON adapter
     * @param publicKey  public key for verification
     * @param privateKey private key for signing (nullable for verify-only)
     */
    public EdDSAJwtCodec(Json json, PublicKey publicKey, PrivateKey privateKey) {
        super(json, Algorithm.EdDSA);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    /**
     * 🏗️ Construct EdDSA codec with explicit clock and skew.
     *
     * @param json       JSON adapter
     * @param publicKey  public key for verification
     * @param privateKey private key for signing (nullable for verify-only)
     * @param clock      time source for validation
     * @param skew       allowed clock skew
     */
    public EdDSAJwtCodec(Json json, PublicKey publicKey, PrivateKey privateKey, Clock clock, long skew) {
        super(json, Algorithm.EdDSA, clock, skew);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    /**
     * ✍️ Sign the JOSE signing input with Ed25519.
     *
     * @param signingInput bytes of {@code base64url(header) + "." + base64url(payload)}
     * @return raw Ed25519 signature (64 bytes)
     * @throws Exception if signing fails or no private key is configured
     */
    @Override
    protected byte[] sign(byte[] signingInput) throws Exception {
        if (privateKey == null) {
            throw new JwtValidationException("Encode not supported: no private key");
        }

        Signature signature = Signature.getInstance(ED_25519);

        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    /**
     * ✅ Verify a raw Ed25519 signature using the provided public key.
     *
     * @param signingInput bytes of {@code base64url(header) + "." + base64url(payload)}
     * @param signature    Ed25519 raw signature (64 bytes)
     * @return true if signature is valid, false otherwise
     * @throws Exception if verification process fails
     */
    @Override
    protected boolean verify(byte[] signingInput, byte[] signature) throws Exception {
        Signature s = Signature.getInstance(ED_25519);

        s.initVerify(publicKey);
        s.update(signingInput);

        return s.verify(signature);
    }
}
