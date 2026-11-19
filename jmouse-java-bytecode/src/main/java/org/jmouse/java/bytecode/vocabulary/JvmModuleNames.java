package org.jmouse.java.bytecode.vocabulary;

import java.util.Objects;

/** Decodes the JVMS module-name escaping, without applying Java source identifier restrictions. */
public final class JvmModuleNames {
    private JvmModuleNames() {}

    public static String decode(String encoded) {
        Objects.requireNonNull(encoded, "encoded");
        if (encoded.isEmpty()) {
            throw new IllegalArgumentException("An encoded module name cannot be empty");
        }
        StringBuilder decoded = new StringBuilder(encoded.length());
        for (int index = 0; index < encoded.length(); index++) {
            char character = encoded.charAt(index);
            if (character >= JvmModuleNameToken.CONTROL.minimum()
                    && character <= JvmModuleNameToken.CONTROL.maximum()) {
                throw new IllegalArgumentException("Control character in module name");
            }
            if (character == JvmModuleNameToken.ESCAPE.code()) {
                if (++index == encoded.length()) {
                    throw new IllegalArgumentException("Unterminated escape in module name");
                }
                character = encoded.charAt(index);
                if (character != JvmModuleNameToken.ESCAPE.code()
                        && character != JvmModuleNameToken.COLON.code()
                        && character != JvmModuleNameToken.AT_SIGN.code()) {
                    throw new IllegalArgumentException("Invalid escape in module name");
                }
            } else if (character == JvmModuleNameToken.COLON.code()
                    || character == JvmModuleNameToken.AT_SIGN.code()) {
                throw new IllegalArgumentException("Unescaped reserved character in module name");
            }
            decoded.append(character);
        }
        return decoded.toString();
    }
}
