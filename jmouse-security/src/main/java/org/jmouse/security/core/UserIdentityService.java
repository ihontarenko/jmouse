package org.jmouse.security.core;

public interface UserIdentityService {

    UserIdentity loadUserByUsername(String username) throws UserIdentityNotFoundException;

}
