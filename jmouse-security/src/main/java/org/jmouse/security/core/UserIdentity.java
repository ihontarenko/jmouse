package org.jmouse.security.core;

import java.util.Collection;

/**
 * 👤 User model used during authentication/authorization.
 */
public interface UserIdentity {

    static UserIdentity create(String username, String password, Collection<Authority> authorities, boolean enabled) {
        return new User(username, password, authorities, enabled);
    }

    String username();

    String password();

    Collection<? extends Authority> authorities();

    boolean accountNonExpired();

    boolean accountNonLocked();

    boolean credentialsNonExpired();

    boolean enabled();

    record User(
            String username,
            String password,
            Collection<? extends Authority> authorities,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean enabled
    ) implements UserIdentity {

        public User(String username,
                    String password,
                    Collection<? extends Authority> authorities,
                    boolean enabled
        ) {
            this(username, password, authorities, false, false, false, enabled);
        }

    }

}