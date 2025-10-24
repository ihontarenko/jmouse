package org.jmouse.security;

public interface UserPrincipalRepository {

    UserPrincipal loadUser(String username) throws UserPrincipalNotFoundException;

    void createUser(UserPrincipal user);

    void updateUser(UserPrincipal user);

    void deleteUser(String username);

}
