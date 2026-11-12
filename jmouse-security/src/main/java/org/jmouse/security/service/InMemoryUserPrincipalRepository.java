package org.jmouse.security.service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.UserPrincipalNotFoundException;
import org.jmouse.security.UserPrincipalRepository;

/** Single-process repository with atomic create/replace and immutable principal snapshots. */
public class InMemoryUserPrincipalRepository implements UserPrincipalRepository {

    private final Map<String, UserPrincipal> users = new ConcurrentHashMap<>();

    @Override
    public UserPrincipal loadUser(String username) throws UserPrincipalNotFoundException {
        UserPrincipal principal = users.get(Objects.requireNonNull(username, "username"));
        if (principal == null) {
            throw new UserPrincipalNotFoundException(username);
        }
        return principal;
    }

    @Override
    public void createUser(UserPrincipal user) {
        UserPrincipal snapshot = snapshot(user);
        if (users.putIfAbsent(snapshot.username(), snapshot) != null) {
            throw new IllegalStateException("User already exists");
        }
    }

    @Override
    public void updateUser(UserPrincipal user) {
        UserPrincipal snapshot = snapshot(user);
        if (users.replace(snapshot.username(), snapshot) == null) {
            throw new UserPrincipalNotFoundException(snapshot.username());
        }
    }

    @Override
    public void deleteUser(String username) {
        users.remove(Objects.requireNonNull(username, "username"));
    }

    private UserPrincipal snapshot(UserPrincipal user) {
        Objects.requireNonNull(user, "user");
        if (user.username() == null || user.username().isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return UserPrincipal.User.builder(user).build();
    }
}
