package org.jmouse.security.authentication;

import org.jmouse.security.Authentication;

import java.util.Collection;
import java.util.List;

public class ResolversAuthenticationManager extends AbstractAuthenticationManager
        implements AuthenticationResolverRegistry {

    public ResolversAuthenticationManager(List<AuthenticationResolver> resolvers) {
        super(resolvers);
    }

    @Override
    public Authentication doAuthenticate(Authentication before) throws AuthenticationException {
        return resolveResolver(before.getClass()).authenticate(before);
    }

    @Override
    public Collection<? extends AuthenticationResolver> getResolvers() {
        return List.copyOf(resolvers);
    }

    @Override
    public void addResolver(AuthenticationResolver resolver) {
        resolvers.add(resolver);
    }

}
