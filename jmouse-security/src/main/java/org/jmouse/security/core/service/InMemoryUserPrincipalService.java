package org.jmouse.security.core.service;

import org.jmouse.core.Streamable;
import org.jmouse.security.core.PasswordMismatchException;
import org.jmouse.security.core.UserPrincipal;
import org.jmouse.security.core.UserPrincipal.User;
import org.jmouse.security.core.UserPrincipalRepository;
import org.jmouse.security.core.UserPrincipalService;

public class InMemoryUserPrincipalService extends InMemoryUserPrincipalRepository
        implements UserPrincipalService, UserPrincipalRepository {

    public InMemoryUserPrincipalService(UserPrincipal... users) {
        Streamable.of(users).forEach(this::createUser);
    }

    @Override
    public void changePassword(UserPrincipal principal, String oldPassword, String newPassword) {
        UserPrincipal user = loadUser(principal.getName());

        if (!oldPassword.equals(user.password())) {
            throw new PasswordMismatchException(principal.getName());
        }

        updateUser(User.builder(user).password(newPassword).build());
    }

    @Override
    public boolean userExists(String username) {
        try {
            loadUser(username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
