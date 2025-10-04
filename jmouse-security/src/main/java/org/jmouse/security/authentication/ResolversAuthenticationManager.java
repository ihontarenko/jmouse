package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

import java.util.List;

public class ResolversAuthenticationManager extends AbstractAuthenticationManager {

    public ResolversAuthenticationManager(List<AuthenticationResolver> resolvers) {
        super(resolvers);
    }

    @Override
    public Authentication doAuthenticate(Authentication before) throws AuthenticationException {
        Authentication after = null;

        for (AuthenticationResolver resolver : resolvers) {
            if (resolver.supports(before.getClass())) {
                if ((after = resolver.authenticate(before)) != null) {
                    break;
                }
            }
        }

        if (after == null) {
            throw new AuthenticationException("NO SUITABLE RESOLVER FOUND!");
        }

        return after;
    }

}
