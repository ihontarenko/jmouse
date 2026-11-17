package org.jmouse.json.syntax;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * 🧾 The tokens JSON is made of, as the engine names them: the marks {@code { } [ ] : ,}, {@code -}, and the words
 * {@code true}, {@code false} and {@code null}. Anything else the engine recognizes is no JSON, and the parsers refuse it.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonRecognizer extends CompositeRecognizer {

    public JsonRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 1000));
    }
}
