package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

public record JvmTextInput(TokenCursor cursor) {
    public int offset() {
        return cursor.peek().offset();
    }

    public boolean atEnd() {
        return cursor.isCurrent(JvmTextToken.END);
    }

    public boolean matches(JvmSignatureToken terminal) {
        return cursor.isCurrent(terminal);
    }

    public Token next(ParseSession session) {
        if (atEnd()) {
            throw failure("Unexpected end of JVM type text");
        }
        session.step();
        return cursor.next();
    }

    public boolean take(JvmSignatureToken terminal, ParseSession session) {
        if (!matches(terminal)) {
            return false;
        }
        next(session);
        return true;
    }

    public void expect(JvmSignatureToken terminal, ParseSession session) {
        if (!take(terminal, session)) {
            throw failure("Expected JVM terminal " + terminal);
        }
    }

    public String slice(int start, int end) {
        return cursor.source().subSequence(start, end).toString();
    }

    public IllegalArgumentException failure(String message) {
        return new IllegalArgumentException(
                cursor.source().getName() + ":" + offset() + ": " + message);
    }
}
