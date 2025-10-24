package org.jmouse.security.password;

public interface PasswordEncoder {

    default String encode(String password) {
        return encode(password.toCharArray());
    }

    default boolean verify(String password, String hash) {
        return verify(password.toCharArray(), hash);
    }

    String encode(char[] password);

    boolean verify(char[] password, String hash);

}
