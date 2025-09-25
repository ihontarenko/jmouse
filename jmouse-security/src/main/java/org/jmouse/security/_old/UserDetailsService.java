package org.jmouse.security._old;

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
