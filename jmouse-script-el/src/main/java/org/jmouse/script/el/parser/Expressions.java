package org.jmouse.script.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;

/**
 * The one way an expression is read in this language.
 *
 * <p>⚠️ <strong>One parser, every position.</strong> A {@code when} guard, an {@code if} condition, a
 * {@code local} value, the right-hand side of an assignment and the collection a loop walks are all
 * read through here, so {@code and}, {@code or}, {@code ==}, {@code is} and {@code in} mean exactly one
 * thing wherever they are written.</p>
 *
 * <p>The alternative is how a dialect ends up with two expression languages: a guard parsed one way
 * because it is a guard, a value another because it is a value, and a script whose author has to
 * remember which half of the file they are in. This class exists so that decision has one site rather
 * than a dozen call sites that each looked reasonable in isolation.</p>
 *
 * <p>⚠️ It is jmouse-el <strong>as it already is</strong>, Lua's spellings included out. Negation is
 * {@code !}, not {@code not} — {@code not} is already this engine's second spelling of {@code !=} and
 * the partner of {@code is}, so admitting it as a prefix would make {@code a not b} and {@code not a}
 * two different operators wearing one word.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Expressions {

    private Expressions() {
    }

    /**
     * Reads one expression from the cursor.
     *
     * @param cursor  the cursor, positioned on the expression's first token
     * @param context the parser context
     * @return the compiled expression
     */
    public static Expression read(TokenCursor cursor, ParserContext context) {
        return (Expression) context.getParser(ExpressionParser.class).parse(cursor, context);
    }
}
