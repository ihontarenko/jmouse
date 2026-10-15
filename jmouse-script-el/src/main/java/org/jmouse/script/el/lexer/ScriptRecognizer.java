package org.jmouse.script.el.lexer;

import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;
import org.jmouse.script.el.lexer.StatementRecognizer;

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
public class ScriptRecognizer extends StatementRecognizer {

    public ScriptRecognizer() {
        addRecognizer(new EnumTokenRecognizer<>(ScriptToken.class, 900));
    }

}
