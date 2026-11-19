package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

/** JVMS identifiers deliberately admit more characters than Java source identifiers. */
public final class JvmSignatureIdentifiers {
    private JvmSignatureIdentifiers() {}

    public static String read(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        while (!input.atEnd() && !delimiter(input)) {
            input.next(session);
        }
        if (input.offset() == start) {
            throw input.failure("Expected a non-empty JVM signature identifier");
        }
        return input.slice(start, input.offset());
    }

    /** Distinguishes an absent class bound followed by a formal whose name starts with L or T. */
    public static boolean startsTypeParameter(JvmTextInput input, ParseSession session) {
        if (!input.matches(JvmSignatureToken.CLASS)
                && !input.matches(JvmSignatureToken.TYPE_VARIABLE)) {
            return false;
        }
        TokenCursor.Savepoint checkpoint = input.cursor().savepoint();
        try {
            read(input, session);
            return input.matches(JvmSignatureToken.BOUND);
        } finally {
            input.cursor().restore(checkpoint);
        }
    }

    private static boolean delimiter(JvmTextInput input) {
        return input.matches(JvmSignatureToken.INNER_TYPE)
                || input.matches(JvmSignatureToken.SEMICOLON)
                || input.matches(JvmSignatureToken.ARRAY)
                || input.matches(JvmSignatureToken.PACKAGE_SEPARATOR)
                || input.matches(JvmSignatureToken.ARGUMENTS_OPEN)
                || input.matches(JvmSignatureToken.ARGUMENTS_CLOSE)
                || input.matches(JvmSignatureToken.BOUND);
    }
}
