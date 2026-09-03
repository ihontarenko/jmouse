package org.jmouse.script.el.lexer;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;

/**
 * Recognizes the three vocabularies a {@code .jms} file is written in.
 *
 * <p>Order matters and runs from most general to most specific: a word is a {@link BasicToken} first,
 * then a {@link LanguageToken}, and only then one of the script language's own {@link ScriptToken}s.
 * That way jMS can add keywords without any of them shadowing a word the expression language already
 * answers to — {@code in}, {@code and}, {@code or}, {@code is} stay exactly what they were.</p>
 *
 * <p>The same house as {@code AccessRecognizer} for {@code .jmp} and {@code JmmRecognizer} for
 * {@code .jmm}. Three dialects reading their keywords in three different orders would be three
 * different expression languages wearing one name.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptRecognizer extends CompositeRecognizer {

    public ScriptRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        addRecognizer(new EnumTokenRecognizer<>(LanguageToken.class, 2000));
        addRecognizer(new EnumTokenRecognizer<>(ScriptToken.class, 1000));
    }

    @Override
    public int order() {
        return -10;
    }

}
