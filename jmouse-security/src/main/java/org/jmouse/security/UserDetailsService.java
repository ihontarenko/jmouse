package org.jmouse.security;

import java.util.Set;

public interface UserDetailsService {

    UserDetails loadUserByUsername(String username);

    interface UserDetails {

        String username();

        String password();

        Set<String> authorities();

        boolean enabled();

    }
}
