package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.lexer.ScriptToken;
import org.jmouse.script.el.node.HandlerNode;

/**
 * Parses {@code on <event> [<literal>] [when <expression>] do … end}.
 *
 * <h2>⚠️ The event name is read, not checked</h2>
 *
 * <p>Which events exist is the host's to declare, so this parser accepts any name and the binder
 * refuses the ones nobody declared — with a file, a line and a column. A parser that held a list of
 * events would be a parser with a product in it.</p>
 *
 * <p>The optional literal after the name — the {@code 180} of {@code on timer 180} — is read as an
 * ordinary jmouse-el literal and carried uninterpreted. Whether it is a period, a count or a channel is
 * the host's business; the language only has to not lose it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.HANDLER)
public class HandlerParser extends AbstractBodyParser<HandlerNode, ScriptToken> {

    @Override
    protected HandlerNode createNode(TokenCursor cursor, ParserContext context) {
        HandlerNode node = new HandlerNode();

        node.setEvent(SourceReader.name(cursor));

        if (org.jmouse.el.CursorMatcher.literal().matches(cursor)) {
            node.setArgument((Expression) context.getParser(LiteralParser.class).parse(cursor, context));
        }

        if (cursor.consumeIf(ScriptToken.T_WHEN)) {
            node.setCondition(Expressions.read(cursor, context));
        }

        cursor.ensure(ScriptToken.T_DO);

        return node;
    }

    @Override
    protected ScriptToken token() {
        return ScriptToken.T_ON;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.handler().matches(cursor);
    }

    @Override
    protected Class<? extends Parser> statementsParser() {
        return ScriptBodyParser.class;
    }

    @Override
    protected void closeBlock(TokenCursor cursor) {
        cursor.ensure(ScriptToken.T_END);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        return (S) SourceReader.span(cursor);
    }

}
