package org.jmouse.security.jwt;

import org.jmouse.security.jwt.codec.HS256JwtCodec;
import org.jmouse.security.jwt.codec.RS256JwtCodec;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

/**
 * 🏭 Simple registry-based factory for JwtCodec.
 * <p>
 * Usage:
 * <pre>
 * {@code JwtCodecFactory f = JwtCodecFactory.builder(json)
 * .hs256(secret)
 * .rs256(publicKey, privateKey)
 * .build();
 * JwtCodec codec = f.getCodec(JwtCodec.Algorithm.HS256);}
 * </pre>
 */
public final class JwtCodecFactory {

    private final Map<JwtCodec.Algorithm, JwtCodec> registry;

    private JwtCodecFactory(Map<JwtCodec.Algorithm, JwtCodec> registry) {
        this.registry = registry;
    }

    public static Builder builder(JwtCodec.Json json) {
        return new Builder(json);
    }

    public JwtCodec getCodec(JwtCodec.Algorithm algorithm) {
        JwtCodec codec = registry.get(algorithm);
        if (codec == null) {
            throw new IllegalStateException("No codec registered for " + algorithm);
        }
        return codec;
    }

    public static final class Builder {

        private final JwtCodec.Json                     json;
        private final Map<JwtCodec.Algorithm, JwtCodec> reg         = new EnumMap<>(JwtCodec.Algorithm.class);
        private       Clock                             clock       = Clock.systemUTC();
        private       long                              skewSeconds = 60;

        private Builder(JwtCodec.Json json) {
            this.json = json;
        }

        /**
         * ⏱️ Optional tuning.
         */
        public Builder clock(java.time.Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder allowedSkewSeconds(long seconds) {
            this.skewSeconds = seconds;
            return this;
        }

        public Builder hs256(byte[] secret) {
            reg.put(JwtCodec.Algorithm.HS256, new HS256JwtCodec(
                    new Adapter(), secret, clock, skewSeconds));
            return this;
        }

        public Builder rs256(PublicKey publicKey, PrivateKey privateKey) {
            reg.put(JwtCodec.Algorithm.RS256, new RS256JwtCodec(
                    new Adapter(), publicKey, privateKey, clock, skewSeconds));
            return this;
        }

        public JwtCodecFactory build() {
            return new JwtCodecFactory(Map.copyOf(reg));
        }
    }

    /**
     * 🔗 Adapter to reuse single Json mapper interface in abstract base.
     */
    private static final class Adapter implements AbstractJwtCodec.Json {

        @Override
        public Map<String, Object> readObject(String json) {
            return Map.of();
        }

        @Override
        public String writeObject(Map<String, Object> object) {
            return "";
        }

    }
}
