package org.jmouse.security.authentication.jwt;

import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authentication.AuthenticationResolver;
import org.jmouse.security.authentication.BearerTokenAuthentication;
import org.jmouse.security.Authentication;
import org.jmouse.security.Authority;
import org.jmouse.security.jwt.Jwt;
import org.jmouse.security.jwt.JwtAuthorities;
import org.jmouse.security.jwt.JwtCodec;
import org.jmouse.security.jwt.ValidatingJwtCodec;

import java.util.Collection;
import java.util.function.Function;

public class JwtTokenAuthenticationResolver implements AuthenticationResolver {

    private final JwtCodec                             codec;
    private       Function<Jwt, Collection<Authority>> authorities = JwtAuthorities.scopeAuthorities();

    public JwtTokenAuthenticationResolver(JwtCodec codec) {
        this.codec = codec;
    }

    public JwtTokenAuthenticationResolver authorities(Function<Jwt, Collection<Authority>> function) {
        this.authorities = function;
        return this;
    }

    @Override
    public boolean supports(Class<?> authenticationType) {
        return BearerTokenAuthentication.class.isAssignableFrom(authenticationType);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String                credentials = String.valueOf(authentication.getCredentials());
        Jwt                   jwt         = codec.decode(credentials);

        if (!(codec instanceof ValidatingJwtCodec)) {
            // verify
        }

        Collection<Authority> authorities = this.authorities.apply(jwt);
        return new BearerTokenAuthentication(jwt.subject(), jwt, authorities);
    }
}
