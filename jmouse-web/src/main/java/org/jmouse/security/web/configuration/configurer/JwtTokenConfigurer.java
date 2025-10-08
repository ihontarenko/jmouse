package org.jmouse.security.web.configuration.configurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.authentication.AuthenticationResolverRegistry;
import org.jmouse.security.authentication.jwt.JwtTokenAuthenticationResolver;
import org.jmouse.security.jwt.JwtCodec;
import org.jmouse.security.jwt.codec.HS256JwtCodec;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.*;
import org.jmouse.security.web.authentication.bearer.BearerTokenAuthenticationEntryPoint;
import org.jmouse.security.web.authentication.jwt.JwtAuthenticationFilter;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.jwt.JacksonJsonMapper;
import org.jmouse.web.http.request.WWWAuthenticate.Bearer.ErrorCode;

import java.time.Clock;

public final class JwtTokenConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, JwtTokenConfigurer<B>> {

    private final AbstractRealmAuthenticationEntryPoint entryPoint           = new BearerTokenAuthenticationEntryPoint();
    private final EntryPointConfigurer entryPointConfigurer = new EntryPointConfigurer();
    private final CodecConfigurer      jwtCodecConfigurer   = new CodecConfigurer();

    public JwtTokenConfigurer<B> entryPoint(Customizer<EntryPointConfigurer> customizer) {
        customizer.customize(entryPointConfigurer);
        return JwtTokenConfigurer.this;
    }

    public JwtTokenConfigurer<B> codec(Customizer<CodecConfigurer> customizer) {
        customizer.customize(jwtCodecConfigurer);
        return JwtTokenConfigurer.this;
    }

    public JwtTokenConfigurer<B> realm(String realm) {
        entryPointConfigurer.realmName(realm);
        return this;
    }

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository repository,
            RequestMatcher matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        if (entryPoint instanceof BearerTokenAuthenticationEntryPoint bearerEntryPoint) {
            bearerEntryPoint.setRealmName(entryPointConfigurer.realmName());
            bearerEntryPoint.setErrorDescription(entryPointConfigurer.errorDescription());
            bearerEntryPoint.setErrorCode(entryPointConfigurer.errorCode().name());
        }

        if (authenticationManager instanceof AuthenticationResolverRegistry resolverRegistry) {
            JwtCodec codec = jwtCodecConfigurer.codec();

            if (codec == null) {
                throw new IllegalStateException("Required JWT codec not configured yet.");
            }

            if (!resolverRegistry.hasResolver(JwtTokenAuthenticationResolver.class)) {
                resolverRegistry.addResolver(new JwtTokenAuthenticationResolver(
                        jwtCodecConfigurer.codec()
                ));
            }
        }

        AuthenticationFailureHandler failure = failureHandler != null
                ? failureHandler : entryPoint::initiate;
        AuthenticationSuccessHandler success = successHandler != null
                ? successHandler : new NoopHttp200SuccessHandler();

        return new JwtAuthenticationFilter(authenticationManager, repository, matcher, success, failure);
    }

    public static class CodecConfigurer {

        private JwtCodec             codec;
        private JwtCodec.AdapterJson adapter = new JacksonJsonMapper(new ObjectMapper());

        public JwtCodec codec() {
            return codec;
        }

        public CodecConfigurer codec(JwtCodec jwtCodec) {
            this.codec = jwtCodec;
            return this;
        }

        public CodecConfigurer adapter(JwtCodec.AdapterJson adapterJson) {
            this.adapter = adapterJson;
            return this;
        }

        public CodecConfigurer hs256(Customizer<Hs256JwtCodecConfigurer> customizer) {
            Hs256JwtCodecConfigurer configurer = new Hs256JwtCodecConfigurer();
            customizer.customize(configurer);
            this.codec = configurer.build();
            return this;
        }

        public CodecConfigurer rs256(Customizer<Rs256JwtCodecConfigurer> customizer) {
            Rs256JwtCodecConfigurer configurer = new Rs256JwtCodecConfigurer();
            customizer.customize(configurer);
//            this.codec = configurer.build();
            return this;
        }

        public CodecConfigurer ed25519(Customizer<EdDSAJwtCodecConfigurer> customizer) {
            EdDSAJwtCodecConfigurer configurer = new EdDSAJwtCodecConfigurer();
            customizer.customize(configurer);
//            this.codec = configurer.build();
            return this;
        }

        public CodecConfigurer es256(Customizer<Es256JwtCodecConfigurer> customizer) {
            Es256JwtCodecConfigurer configurer = new Es256JwtCodecConfigurer();
            customizer.customize(configurer);
//            this.codec = configurer.build();
            return this;
        }

        public class Hs256JwtCodecConfigurer {

            private byte[] secret;
            private long   skew;
            private Clock  clock;

            public Hs256JwtCodecConfigurer secret(String secret) {
                return secret(secret.getBytes());
            }

            public Hs256JwtCodecConfigurer secret(byte[] secret) {
                this.secret = secret.clone();
                return this;
            }

            public Hs256JwtCodecConfigurer skew(long skew) {
                this.skew = skew;
                return this;
            }

            public Hs256JwtCodecConfigurer clock(Clock clock) {
                this.clock = clock;
                return this;
            }

            public JwtCodec build() {
                return new HS256JwtCodec(CodecConfigurer.this.adapter, secret, clock, skew);
            }

        }

        public class Rs256JwtCodecConfigurer {
            // todo: not implemented yet
        }

        public class EdDSAJwtCodecConfigurer {
            // todo: not implemented yet
        }

        public class Es256JwtCodecConfigurer {
            // todo: not implemented yet
        }

    }


    public static class EntryPointConfigurer {

        private String    realmName;
        private String    errorDescription;
        private ErrorCode errorCode;

        public String realmName() {
            return realmName;
        }

        public EntryPointConfigurer realmName(String realmName) {
            this.realmName = realmName;
            return this;
        }

        public String errorDescription() {
            return errorDescription;
        }

        public EntryPointConfigurer errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public ErrorCode errorCode() {
            return errorCode;
        }

        public EntryPointConfigurer errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

    }

}
