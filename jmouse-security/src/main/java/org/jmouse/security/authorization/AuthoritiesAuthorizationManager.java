package org.jmouse.security.authorization;

import java.util.Collection;
import java.util.Set;
import org.jmouse.security.Authentication;
import org.jmouse.security.Authority;
import org.jmouse.security.access.RoleHierarchy;

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
        return new AuthoritiesAccessResult(
            isAuthorized(authentication, authorities),
            Authority.ofCollection(authorities)
        );
    }

    private boolean isAuthorized(Authentication authentication, Collection<String> authorities) {
        if (
            authentication == null ||
            !authentication.isAuthenticated() ||
            authentication.level() == org.jmouse.security.authentication.AuthenticationLevel.ANONYMOUS
        ) {
            return false;
        }
        Set<Authority> granted = getRoleHierarchy().getReachableRoles(authentication.getAuthorities());

        for (String authority : authorities) {
            if (granted.contains(Authority.of(authority))) {
                return true;
            }
        }

        return false;
    }
}
