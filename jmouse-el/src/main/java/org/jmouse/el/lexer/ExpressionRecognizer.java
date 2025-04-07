package org.jmouse.el.lexer;

import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

public class ExpressionRecognizer extends CompositeRecognizer {

    public ExpressionRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 2000));
    }

}
