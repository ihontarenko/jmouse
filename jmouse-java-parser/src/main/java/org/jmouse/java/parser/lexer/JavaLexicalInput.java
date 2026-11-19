package org.jmouse.java.parser.lexer;

import java.util.Objects;
import org.jmouse.java.model.version.JavaSourceProfile;

/** A recognizer sees Unicode-translated characters; emission maps them back to the original source. */
public record JavaLexicalInput(String text, int offset, JavaSourceProfile profile) {
    public JavaLexicalInput {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(profile, "profile");
        Objects.checkFromToIndex(offset, offset, text.length());
    }

    public boolean startsWith(String spelling) {
        return text.startsWith(spelling, offset);
    }
}
