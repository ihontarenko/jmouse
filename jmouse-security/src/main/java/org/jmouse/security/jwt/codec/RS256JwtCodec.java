package org.jmouse.security.jwt.codec;

import org.jmouse.security.jwt.AbstractJwtCodec;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;

/**
 * 🔑 RS256 codec (RSA-PKCS#1 v1.5 with SHA-256).
 */
public final class RS256JwtCodec extends AbstractJwtCodec {

    public static final String SHA_256_WITH_RSA = "SHA256withRSA";

    private final PublicKey  publicKey;
    private final PrivateKey privateKey;

    public RS256JwtCodec(AdapterJson json, PublicKey publicKey, PrivateKey privateKey) {
        super(json, Algorithm.RS256);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public RS256JwtCodec(AdapterJson json, PublicKey publicKey, PrivateKey privateKey, Clock clock, long skewSeconds) {
        super(json, Algorithm.RS256, clock, skewSeconds);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    @Override
    protected byte[] sign(byte[] signingInput) throws Exception {
        if (privateKey == null) {
            throw new JwtValidationException("Encode not supported: no private key configured");
        }

        Signature signature = Signature.getInstance(SHA_256_WITH_RSA);

        signature.initSign(privateKey);
        signature.update(signingInput);

        return signature.sign();
    }

    @Override
    protected boolean verify(byte[] signingInput, byte[] signature) throws Exception {
        Signature s = Signature.getInstance(SHA_256_WITH_RSA);

        s.initVerify(publicKey);
        s.update(signingInput);

        return s.verify(signature);
    }
}
