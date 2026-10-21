package org.jmouse.datamap.lexer;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * The expression language's token kinds, and jMD's three words before them. jMD writes with the expression language's
 * characters, so it is cut by its splitter; only the words differ.
 */
public class DataMapRecognizer extends CompositeRecognizer {

    public DataMapRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(DataMapToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }
}
