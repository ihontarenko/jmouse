package org.jmouse.security.authentication.dao;

import org.jmouse.security.authentication.AbstractUsernamePasswordAuthenticationResolver;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.Authentication;
import org.jmouse.security.UserPrincipal;

public class DaoAuthenticationResolver extends AbstractUsernamePasswordAuthenticationResolver {

    @Override
    protected Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof UsernamePasswordAuthentication usernamePassword) {
            UserPrincipal principal = getPrincipalService().loadUser(usernamePassword.getName());
            UsernamePasswordAuthentication authenticated = new UsernamePasswordAuthentication(
                    principal, principal.password(), principal.authorities());
            authenticated.setAuthenticated(true);
        }

        throw new AuthenticationException("Unsupported authentication type!");
    }

}
