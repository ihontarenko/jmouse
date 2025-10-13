package org.jmouse.security.core;

public interface UserPrincipalService extends UserPrincipalRepository {

    void changePassword(UserPrincipal principal, String oldPassword, String newPassword);

    boolean userExists(String username);

}
