package org.jmouse.java.parser.lexer;

import org.jmouse.el.lexer.Token;
import org.jmouse.java.parser.source.ProblemKind;

/** Offsets are relative to one recognized region, bounded by its consumed length. */
public interface JavaLexicalOutput {
    void token(Token.Type type, String value, int start, int end);

    void comment(JavaComment.Kind kind, int start, int end);

    void problem(ProblemKind kind, String message, int start, int end);
}
