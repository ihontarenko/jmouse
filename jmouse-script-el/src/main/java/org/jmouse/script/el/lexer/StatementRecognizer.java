package org.jmouse.script.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * Recognizes the expression language's tokens and the statement keywords. A dialect extends it and adds its own
 * keyword set on top.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StatementRecognizer extends CompositeRecognizer {

    public StatementRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(LanguageToken.class, 2000));
        addRecognizer(new EnumTokenRecognizer<>(StatementToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }

}
