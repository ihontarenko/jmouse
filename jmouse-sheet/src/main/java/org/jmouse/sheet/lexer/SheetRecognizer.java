package org.jmouse.sheet.lexer;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * The expression language's token kinds, and sheet's three words before them. sheet writes with the expression language's
 * characters, so it is cut by its splitter; only the words differ.
 */
public class SheetRecognizer extends CompositeRecognizer {

    public SheetRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(SheetToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }
}
