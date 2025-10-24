package org.jmouse.security.service;

import org.jmouse.security.UserPrincipal;
import org.jmouse.security.UserPrincipalNotFoundException;
import org.jmouse.security.UserPrincipalRepository;

import java.util.HashMap;
import java.util.Map;

public class InMemoryUserPrincipalRepository implements UserPrincipalRepository {

    private final Map<String, UserPrincipal> users = new HashMap<>();

    @Override
    public UserPrincipal loadUser(String username) throws UserPrincipalNotFoundException {
        UserPrincipal principal = users.get(username);

        if (principal == null) {
            throw new UserPrincipalNotFoundException(username);
        }

        return principal;
    }

    @Override
    public void createUser(UserPrincipal user) {
        users.put(user.getName(), user);
    }

    @Override
    public void updateUser(UserPrincipal user) {
        UserPrincipal oldPrincipal = loadUser(user.getName());
        UserPrincipal newPrincipal = UserPrincipal.User.builder(user).build();
        deleteUser(oldPrincipal.getName());
        createUser(newPrincipal);
    }

    @Override
    public void deleteUser(String username) {
        users.remove(username);
    }

}
