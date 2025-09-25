package org.jmouse.security.authentication;

import org.jmouse.security.core.Authentication;

import java.util.List;

public class ProviderAuthenticationManager extends AbstractAuthenticationManager {

    public ProviderAuthenticationManager(List<AuthenticationProvider> providers) {
        super(providers);
    }

    @Override
    public Authentication authenticate(Authentication request) throws AuthenticationException {
        Authentication result = null;

        for (AuthenticationProvider provider : providers) {
            if (provider.supports(request.getClass())) {
                if ((result = provider.authenticate(request)) != null) {
                    break;
                }
            }
        }

        if (result == null) {
            throw new AuthenticationException("No suitable AuthenticationProvider");
        }

        return result;
    }

}
