package org.jmouse.security;

public interface PasswordEncoder {

    String encode(CharSequence password);

    boolean matches(CharSequence password, String encoded);

}
