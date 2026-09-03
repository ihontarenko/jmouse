package org.jmouse.validator.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * Recognizes the three vocabularies a {@code .jmv} file is written in.
 *
 * <p>Order runs from most general to most specific: a word is a {@link BasicToken} first, then a
 * {@link LanguageToken}, and only then one of the validation language's own {@link JmvToken}s. That way
 * {@code .jmv} adds keywords without any of them shadowing a word the expression language already
 * answers to — which matters here as much as anywhere, because a guard's condition and a check's
 * message <em>are</em> expressions.</p>
 *
 * <p>⚠️ Recognizing a word as a keyword is not the same as the grammar wanting one there. See
 * {@link JmvToken#nameTokens()} for the positions that accept a keyword as an ordinary name, and for
 * why refusing to would make fields that cannot be written down.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JmvRecognizer extends CompositeRecognizer {

    public JmvRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(LanguageToken.class, 2000));
        addRecognizer(new EnumTokenRecognizer<>(JmvToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }
}
