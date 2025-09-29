package org.jmouse.security.authorization;

import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.Authority;
import org.jmouse.security.core.access.RoleHierarchy;

import java.util.Collection;
import java.util.Set;

public class AuthoritiesAuthorizationManager implements AuthorizationManager<Collection<String>> {

    private RoleHierarchy roleHierarchy = RoleHierarchy.none();

    public RoleHierarchy getRoleHierarchy() {
        return roleHierarchy;
    }

    public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    public AccessResult check(Authentication authentication, Collection<String> authorities) {
        return new AuthoritiesAccessResult(isAuthorized(authentication, authorities), Authority.ofCollection(authorities));
    }

    private boolean isAuthorized(Authentication authentication, Collection<String> authorities) {
        Set<Authority> granted = getRoleHierarchy().getReachableRoles(authentication.getAuthorities());

        for (String authority : authorities) {
            if (granted.contains(Authority.of(authority))) {
                return true;
            }
        }

        return false;
    }

}
