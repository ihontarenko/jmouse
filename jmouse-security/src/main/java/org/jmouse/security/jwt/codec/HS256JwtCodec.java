package org.jmouse.security.jwt.codec;

import org.jmouse.security.jwt.AbstractJwtCodec;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;

/**
 * 🔒 HS256 codec (HMAC-SHA256).
 */
public final class HS256JwtCodec extends AbstractJwtCodec {

    public static final String HMAC_SHA_256 = "HmacSHA256";

    private final byte[] secret;

    public HS256JwtCodec(AdapterJson json, byte[] secret) {
        super(json, Algorithm.HS256);
        this.secret = secret.clone();
    }

    public HS256JwtCodec(AdapterJson json, byte[] secret, Clock clock, long skewSeconds) {
        super(json, Algorithm.HS256, clock, skewSeconds);
        this.secret = secret.clone();
    }

    @Override
    protected byte[] sign(byte[] signingInput) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA_256);
        mac.init(new SecretKeySpec(secret, HMAC_SHA_256));
        return mac.doFinal(signingInput);
    }

    @Override
    protected boolean verify(byte[] signingInput, byte[] signature) throws Exception {
        return constantTimeEquals(sign(signingInput), signature);
    }
}
