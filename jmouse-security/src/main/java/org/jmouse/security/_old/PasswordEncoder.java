package org.jmouse.security._old;

public interface PasswordEncoder {

    String encode(CharSequence password);

    boolean matches(CharSequence password, String encoded);

}
