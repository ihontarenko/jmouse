package org.jmouse.security.core;

import org.jmouse.core.Streamable;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface UserPrincipal extends Principal {

    static UserPrincipal create(String username, String password, Collection<Authority> authorities, boolean enabled) {
        return new User(username, password, authorities, enabled);
    }

    String username();

    String password();

    Collection<? extends Authority> authorities();

    boolean enabled();

    record User(
            String username,
            String password,
            Collection<? extends Authority> authorities,
            boolean enabled
    ) implements UserPrincipal {

        public User(Builder builder) {
            this(builder.username, builder.password, builder.authorities, builder.enabled);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Builder builder(UserPrincipal user) {
            return builder()
                    .username(user.username())
                    .password(user.password())
                    .authorities(user.authorities())
                    .enabled(user.enabled());
        }

        @Override
        public String getName() {
            return username();
        }

        public static class Builder {

            private final List<Authority> authorities = new ArrayList<>();
            private       String          username;
            private       String          password;
            private       boolean         enabled     = true;

            public Builder() {
            }

            public Builder username(String username) {
                this.username = username;
                return this;
            }

            public Builder password(String password) {
                this.password = password;
                return this;
            }

            public Builder authority(String authority) {
                return authority(Authority.of(authority));
            }

            public Builder authority(Authority authority) {
                if (authority != null) {
                    this.authorities.add(authority);
                }
                return this;
            }

            public Builder authorities(String... authorities) {
                return authorities(Streamable.of(authorities).map(Authority::of).toList());
            }

            public Builder authorities(Collection<? extends Authority> authorities) {
                this.authorities.clear();
                if (authorities != null) {
                    this.authorities.addAll(authorities);
                }
                return this;
            }

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public User build() {
                return new User(this);
            }

        }

    }

}