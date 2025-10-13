package org.jmouse.security.core;

public interface UserPrincipalRepository {

    UserPrincipal loadUser(String username) throws UserPrincipalNotFoundException;

    void createUser(UserPrincipal user);

    void updateUser(UserPrincipal user);

    void deleteUser(String username);

}
