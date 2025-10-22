package org.jmouse.security;

import jakarta.annotation.security.RolesAllowed;

public class UserService {

    @RolesAllowed({"R_ADMIN", "R_OWNER", "USER", "ROLE_ANONYMOUS"})
    public String upper(String username) {
        return username.toUpperCase();
    }

}
