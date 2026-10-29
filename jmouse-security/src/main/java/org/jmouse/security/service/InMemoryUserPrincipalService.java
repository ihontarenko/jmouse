package org.jmouse.security.service;

import org.jmouse.security.PasswordMismatchException;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.UserPrincipalNotFoundException;
import org.jmouse.security.UserPrincipalService;
import org.jmouse.security.password.PasswordEncoder;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import java.util.Arrays;
import java.util.Objects;

/** In-memory users contain encoded passwords; callers encode initial records explicitly. */
public class InMemoryUserPrincipalService extends InMemoryUserPrincipalRepository implements UserPrincipalService {
    private final PasswordEncoder encoder;

    public InMemoryUserPrincipalService(UserPrincipal... users) {
        this(new Pbkdf2Sha256PasswordEncoder(), users);
    }

    public InMemoryUserPrincipalService(PasswordEncoder encoder, UserPrincipal... users) {
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        for (UserPrincipal user : users) {
            createUser(user);
        }
    }

    @Override
    public void changePassword(UserPrincipal principal, String oldPassword, String newPassword) {
        UserPrincipal stored = loadUser(principal.getName());
        char[] oldValue = Objects.requireNonNull(oldPassword, "oldPassword").toCharArray();
        char[] newValue = Objects.requireNonNull(newPassword, "newPassword").toCharArray();
        try {
            if (!encoder.verify(oldValue, stored.password())) {
                throw new PasswordMismatchException(principal.getName());
            }
            updateUser(UserPrincipal.User.builder(stored).password(encoder.encode(newValue)).build());
        } finally {
            Arrays.fill(oldValue, '\0');
            Arrays.fill(newValue, '\0');
        }
    }

    @Override
    public boolean userExists(String username) {
        try {
            loadUser(username);
            return true;
        } catch (UserPrincipalNotFoundException missing) {
            return false;
        }
    }
}
