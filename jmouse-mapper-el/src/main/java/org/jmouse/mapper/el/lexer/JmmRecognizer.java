package org.jmouse.mapper.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * Recognizes the three vocabularies a {@code .jmm} file is written in.
 *
 * <p>Order runs from most general to most specific: a word is a {@link BasicToken} first, then a
 * {@link LanguageToken}, and only then one of the mapping language's own {@link JmmToken}s. That way
 * {@code .jmm} adds keywords without any of them shadowing a word the expression language already
 * answers to — which matters more here than anywhere, because the right-hand side of every rule
 * <em>is</em> an expression.</p>
 *
 * <p>⚠️ Recognizing a word as a keyword is not the same as the grammar wanting one there. See
 * {@link JmmToken#nameTokens()} for the positions that accept a keyword as an ordinary name, and for
 * why refusing to would make properties that cannot be written down.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JmmRecognizer extends CompositeRecognizer {

    public JmmRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(LanguageToken.class, 2000));
        addRecognizer(new EnumTokenRecognizer<>(JmmToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }

}
