package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.jwt.Jwt;
import org.jmouse.security.jwt.JwtAuthorities;
import org.jmouse.security.jwt.JwtCodec;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.*;
import org.jmouse.security.web.authentication.bearer.BearerTokenAuthenticationEntryPoint;
import org.jmouse.security.web.authentication.jwt.JwtAuthenticationFilter;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.context.SecurityContextRepository;

import java.util.Collection;
import java.util.function.Function;

public final class JwtConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, JwtConfigurer<B>> {

    private final AbstractRealmAuthenticationEntryPoint entryPoint  = new BearerTokenAuthenticationEntryPoint();
    private       JwtCodec                              jwtCodec;
    private       Function<Jwt, Collection<String>>     authorities = JwtAuthorities.scopeClaim();

    public JwtConfigurer<B> entryPoint(Customizer<EntryPointConfigurer> customizer) {
        customizer.customize(new EntryPointConfigurer());
        return JwtConfigurer.this;
    }

    public JwtConfigurer<B> decoder(JwtCodec jwtCodec) {
        this.jwtCodec = jwtCodec;
        return this;
    }

    public JwtConfigurer<B> authorities(Function<Jwt, Collection<String>> function) {
        this.authorities = function;
        return this;
    }

    public JwtConfigurer<B> realm(String realm) {
        entryPoint.setRealmName(realm);
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

        if (jwtCodec == null) {
            throw new IllegalStateException("JwtCodec must be provided");
        }

        AuthenticationFailureHandler          failure    = failureHandler != null
                ? failureHandler : entryPoint::initiate;
        AuthenticationSuccessHandler          success    = successHandler != null
                ? successHandler : new NoopHttp200SuccessHandler();

        return new JwtAuthenticationFilter(authenticationManager, repository, matcher, success, failure);
    }

    public class EntryPointConfigurer {



    }

}
