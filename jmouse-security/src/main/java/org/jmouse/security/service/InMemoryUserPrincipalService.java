package org.jmouse.security.service;

import org.jmouse.core.Streamable;
import org.jmouse.security.PasswordMismatchException;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.UserPrincipal.User;
import org.jmouse.security.UserPrincipalRepository;
import org.jmouse.security.UserPrincipalService;

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
