package org.jmouse.formula.el;

import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.FunctionParser;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PrimaryExpressionParser;
import org.jmouse.el.parser.PropertyParser;

import java.util.regex.Pattern;

import static org.jmouse.el.lexer.BasicToken.T_CLOSE_PAREN;
import static org.jmouse.el.lexer.BasicToken.T_COMMA;
import static org.jmouse.el.lexer.BasicToken.T_DOT;
import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_NEW_LINE;
import static org.jmouse.el.lexer.BasicToken.T_OPEN_PAREN;

/** jMF's primary expressions: numbers, names, calls and groups, without jME's lambda ambiguity for (x). */
public final class FormulaPrimaryExpressionParser extends PrimaryExpressionParser {
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (CursorMatcher.function().matches(cursor)) {
            parent.add(context.getParser(FunctionParser.class).parse(cursor, context));
        } else if (CursorMatcher.literal().matches(cursor)) {
            parent.add(context.getParser(LiteralParser.class).parse(cursor, context));
        } else if (cursor.isCurrent(T_IDENTIFIER)) {
            parent.add(context.getParser(PropertyParser.class).parse(cursor, context));
        } else if (isWordArgument(cursor) || isWordWithMember(cursor)) {
            // A keyword standing alone as an argument, or before a member, is a word its host reads — a pin called
            // "in": current(in), in.current — not an operator.
            parent.add(context.getParser(PropertyParser.class).parse(cursor, context));
        } else if (cursor.consumeIf(T_OPEN_PAREN)) {
            parent.add(context.getParser(ExpressionParser.class).parse(cursor, context));
            cursor.ensure(T_CLOSE_PAREN);
        } else if (cursor.consumeIf(T_NEW_LINE)) {
            parse(cursor, parent, context);
        } else {
            throw new ParseException("Unexpected token " + cursor.peek() + " at " + cursor.position());
        }
    }

    /** Whether the cursor is on a keyword spelled as a name, alone between a call's parentheses or commas. */
    private static boolean isWordArgument(TokenCursor cursor) {
        return WORD.matcher(cursor.current().value()).matches()
                && cursor.isPrevious(T_OPEN_PAREN, T_COMMA) && cursor.isNext(T_CLOSE_PAREN, T_COMMA);
    }

    /** Whether the cursor is on a keyword spelled as a name, followed by a dot and a member. */
    private static boolean isWordWithMember(TokenCursor cursor) {
        return WORD.matcher(cursor.current().value()).matches() && cursor.checkAt(1, T_DOT)
                && cursor.checkAt(2, T_IDENTIFIER);
    }

    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
}
