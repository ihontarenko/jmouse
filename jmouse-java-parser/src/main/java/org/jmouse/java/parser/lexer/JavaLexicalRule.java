package org.jmouse.java.parser.lexer;

import org.jmouse.el.lexer.recognizer.Recognizer;

/** Java's specialization of the existing jME recognizer contract. Rules must be stateless and shareable. */
@FunctionalInterface
public interface JavaLexicalRule extends Recognizer<JavaLexicalMatch, JavaLexicalInput> {
    @Override
    default int order() {
        return 0;
    }
}
