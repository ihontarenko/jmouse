package org.jmouse.access.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * Recognizes the three vocabularies a policy file is written in.
 *
 * <p>Order matters and runs from most general to most specific: a word is a {@link BasicToken}
 * first, then a {@link LanguageToken}, and only then one of the policy language's own
 * {@link AccessToken}s. That way {@code .jmp} can add keywords without any of them shadowing a word
 * the expression language already answers to.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AccessRecognizer extends CompositeRecognizer {

    public AccessRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(LanguageToken.class, 2000));
        addRecognizer(new EnumTokenRecognizer<>(AccessToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }

}
