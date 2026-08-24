package org.jmouse.query.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * Recognizes the three vocabularies a {@code .jmq} document is written in.
 *
 * <p>Order runs from most general to most specific: a word is a {@link BasicToken} first, then a
 * {@link LanguageToken}, and only then one of this language's own {@link QueryToken}s. That way jMQ
 * adds keywords without any of them shadowing a word the expression language already answers to —
 * {@code in}, {@code is}, {@code contains}, {@code and} and {@code not} keep meaning exactly what they
 * mean everywhere else.</p>
 *
 * <p>⚠️ It is also why {@code function} is not a {@link QueryToken}: {@link LanguageToken} claims that
 * word and is asked first, so a duplicate constant here would never be produced. The shared token is
 * the one this dialect uses.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryRecognizer extends CompositeRecognizer {

    public QueryRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(LanguageToken.class, 2000));
        addRecognizer(new EnumTokenRecognizer<>(QueryToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }
}
