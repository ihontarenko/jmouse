package org.jmouse.security.authentication;

import org.jmouse.security.core.UserPrincipalService;

abstract public class AbstractUsernamePasswordAuthenticationResolver extends AbstractAuthenticationResolver {

    private UserPrincipalService principalService;

    public UserPrincipalService getPrincipalService() {
        return principalService;
    }

    public void setPrincipalService(UserPrincipalService principalService) {
        this.principalService = principalService;
    }

    @Override
    public boolean supports(Class<?> authenticationType) {
        return UsernamePasswordAuthentication.class.isAssignableFrom(authenticationType);
    }

}
