package org.jmouse.security;

public interface UserPrincipalService extends UserPrincipalRepository {

    void changePassword(UserPrincipal principal, String oldPassword, String newPassword);

    boolean userExists(String username);

}
