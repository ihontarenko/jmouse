package org.jmouse.el.core.lexer;

import org.jmouse.el.core.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.core.lexer.recognizer.EnumTokenRecognizer;

public class ExpressionRecognizer extends CompositeRecognizer {

    public ExpressionRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 2000));
    }

}
