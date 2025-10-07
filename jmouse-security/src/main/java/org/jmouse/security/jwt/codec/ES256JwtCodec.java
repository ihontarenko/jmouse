package org.jmouse.security.jwt.codec;

import org.jmouse.security.jwt.AbstractJwtCodec;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import static java.util.Arrays.copyOfRange;

/**
 * 🔐 ES256JwtCodec — ECDSA P-256 (SHA-256) codec.
 *
 * <p>Implements JOSE-compliant signature handling for ES256:
 * <ul>
 *   <li>✍️ {@link #sign(byte[])} — DER → JOSE (r||s) conversion</li>
 *   <li>✅ {@link #verify(byte[], byte[])} — JOSE (r||s) → DER conversion</li>
 * </ul>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>🔑 If {@code privateKey} is {@code null}, encoding is disabled (decode/verify-only).</li>
 *   <li>📏 P-256 uses fixed 32-byte {@code r} and {@code s} (JOSE signature = 64 bytes).</li>
 * </ul>
 */
public final class ES256JwtCodec extends AbstractJwtCodec {
    /**
     * 🧾 JCA signature algorithm for ES256.
     */
    public static final String SHA_256_WITH_ECDSA = "SHA256withECDSA";

    private final PublicKey  publicKey;
    private final PrivateKey privateKey; // null => decode-only

    /**
     * 🏗️ Construct an ES256 codec (default clock/skew in {@link AbstractJwtCodec}).
     *
     * @param json       JSON adapter
     * @param publicKey  public key for verification
     * @param privateKey private key for signing (nullable for verify-only)
     */
    public ES256JwtCodec(AdapterJson json, PublicKey publicKey, PrivateKey privateKey) {
        super(json, Algorithm.ES256);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    /**
     * 🏗️ Construct an ES256 codec with explicit clock and skew.
     *
     * @param json  JSON adapter
     * @param pub   public key for verification
     * @param priv  private key for signing (nullable for verify-only)
     * @param clock time source for validation
     * @param skew  allowed clock skew (seconds/millis as defined upstream)
     */
    public ES256JwtCodec(AdapterJson json, PublicKey pub, PrivateKey priv, java.time.Clock clock, long skew) {
        super(json, Algorithm.ES256, clock, skew);
        this.publicKey = pub;
        this.privateKey = priv;
    }

    /**
     * 🔄 Convert ECDSA signature from ASN.1 DER (SEQUENCE{INTEGER r, INTEGER s})
     * to JOSE raw format {@code r||s} with fixed-size unsigned components.
     *
     * @param der  DER-encoded signature
     * @param size expected size of r/s in bytes (P-256 = 32)
     * @return concatenated {@code r||s} (length = 2*size)
     * @throws Exception if DER encoding is malformed
     */
    private static byte[] derToJose(byte[] der, int size) throws Exception {
        // DER: SEQ { INT r; INT s } — minimal parsing (short-form lengths).
        int position = 0;

        if (der[position++] != 0x30) {
            throw new Exception("Bad DER: expected SEQUENCE");
        }

        int sequenceLength = der[position++] & 0xff;
        if (sequenceLength > der.length - position) {
            throw new Exception("Bad DER: invalid length");
        }

        if (der[position++] != 0x02) {
            throw new Exception("Bad DER: expected INTEGER (r)");
        }
        int        rl = der[position++] & 0xff;
        BigInteger rv = new BigInteger(copyOfRange(der, position, position + rl));
        position += rl;

        if (der[position++] != 0x02) {
            throw new Exception("Bad DER: expected INTEGER (s)");
        }
        int        sl = der[position++] & 0xff;
        BigInteger sv = new BigInteger(copyOfRange(der, position, position + sl));

        byte[] rb = toUnsignedFixed(rv, size);
        byte[] sb = toUnsignedFixed(sv, size);

        byte[] out = new byte[size * 2];
        System.arraycopy(rb, 0, out, 0, size);
        System.arraycopy(sb, 0, out, size, size);
        return out;
    }

    /**
     * 🔄 Convert JOSE raw format {@code r||s} to ASN.1 DER SEQUENCE.
     *
     * @param jose JOSE signature (64 bytes for P-256; 96 for P-384)
     * @return DER-encoded SEQUENCE{INTEGER r, INTEGER s}
     * @throws Exception if size is not expected for ECDSA
     */
    private static byte[] joseToDer(byte[] jose) throws Exception {
        int size = jose.length / 2;

        if (jose.length != 64 && jose.length != 96) {
            throw new Exception("Bad JOSE ECDSA size");
        }

        BigInteger rv = new BigInteger(1, copyOfRange(jose, 0, size));
        BigInteger sv = new BigInteger(1, copyOfRange(jose, size, jose.length));

        byte[] re     = unsignedIntegerDer(rv);
        byte[] se     = unsignedIntegerDer(sv);
        int    length = 2 + re.length + 2 + se.length;

        byte[] der       = new byte[2 + length];
        int    positions = 0;

        der[positions++] = 0x30;             // SEQUENCE
        der[positions++] = (byte) length;    // (short-form, length < 128 assumed)
        der[positions++] = 0x02;             // INTEGER r
        der[positions++] = (byte) re.length;

        System.arraycopy(re, 0, der, positions, re.length);

        positions += re.length;

        der[positions++] = 0x02;             // INTEGER s
        der[positions++] = (byte) se.length;
        System.arraycopy(se, 0, der, positions, se.length);

        return der;
    }

    /**
     * 🧮 Convert {@link BigInteger} to unsigned, left-padded fixed-size byte array.
     *
     * @param value big integer value (two's complement)
     * @param size  target fixed size in bytes
     * @return unsigned fixed-size representation
     */
    private static byte[] toUnsignedFixed(BigInteger value, int size) {
        byte[] byteArray = value.toByteArray(); // two's complement

        if (byteArray.length == size) {
            return byteArray;
        }

        if (byteArray.length == size + 1 && byteArray[0] == 0) {
            return copyOfRange(byteArray, 1, byteArray.length); // strip sign 0x00
        }

        byte[] output = new byte[size];

        System.arraycopy(byteArray, Math.max(0, byteArray.length - size), output, Math.max(0, size - byteArray.length),
                         Math.min(size, byteArray.length));

        return output;
    }

    /**
     * 🧮 Encode a positive INTEGER for DER, prefixing 0x00 when MSB is set.
     *
     * @param value positive integer (treated as unsigned magnitude)
     * @return minimal big-endian DER INTEGER content (without tag/length)
     */
    private static byte[] unsignedIntegerDer(BigInteger value) {
        byte[] byteArray = value.toByteArray();

        if (byteArray[0] == 0) {
            return byteArray;
        }

        if ((byteArray[0] & 0x80) != 0) {
            byte[] prepend = new byte[byteArray.length + 1];
            prepend[0] = 0;
            System.arraycopy(byteArray, 0, prepend, 1, byteArray.length);
            return prepend;
        }

        return byteArray;
    }

    /**
     * ✍️ Sign the JOSE signing input with ES256 and return JOSE raw signature ({@code r||s}).
     *
     * @param signingInput bytes of {@code base64url(header) + "." + base64url(payload)}
     * @return JOSE raw signature (64 bytes for P-256)
     * @throws Exception if signing fails or no private key is configured
     */
    @Override
    protected byte[] sign(byte[] signingInput) throws Exception {
        if (privateKey == null) {
            throw new JwtValidationException("Encode not supported: no private key");
        }

        Signature signature = Signature.getInstance(SHA_256_WITH_ECDSA);
        signature.initSign(privateKey);
        signature.update(signingInput);

        byte[] der = signature.sign();
        return derToJose(der, 32); // r||s (64 bytes)
    }

    /**
     * ✅ Verify a JOSE raw signature by converting to DER and delegating to JCA.
     *
     * @param signingInput bytes of {@code base64url(header) + "." + base64url(payload)}
     * @param signature    JOSE raw signature (r||s)
     * @return true if valid, false otherwise
     * @throws Exception if verification process fails
     */
    @Override
    protected boolean verify(byte[] signingInput, byte[] signature) throws Exception {
        Signature s   = Signature.getInstance(SHA_256_WITH_ECDSA);
        byte[]    der = joseToDer(signature); // convert r||s -> DER
        s.initVerify(publicKey);
        s.update(signingInput);
        return s.verify(der);
    }
}
