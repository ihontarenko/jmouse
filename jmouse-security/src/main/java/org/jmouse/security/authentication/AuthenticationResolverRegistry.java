package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

import java.util.Collection;

public interface AuthenticationResolverRegistry {

    Collection<? extends AuthenticationResolver> getResolvers();

    void addResolver(AuthenticationResolver resolver);

    default boolean hasResolver(Class<? extends AuthenticationResolver> resolverType) {
        return getResolvers().stream().anyMatch(resolverType::isInstance);
    }

    default boolean hasResolver(AuthenticationResolver resolver) {
        return hasResolver(resolver.getClass());
    }

    default AuthenticationResolver resolveResolver(Class<? extends Authentication> authenticationType) {
        AuthenticationResolver resolver = null;

        for (AuthenticationResolver authenticationResolver : getResolvers()) {
            if (authenticationResolver.supports(authenticationType)) {
                resolver = authenticationResolver;
                break;
            }
        }

        if (resolver == null) {
            throw new IllegalArgumentException("NO AUTHENTICATION RESOLVER FOUND FOR: " + authenticationType);
        }

        return resolver;
    }

}
