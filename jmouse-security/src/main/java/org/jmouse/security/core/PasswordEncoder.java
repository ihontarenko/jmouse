package org.jmouse.security.core;

public interface PasswordEncoder {

    String encode(String password);

    boolean matches(String password, String hash);

}
