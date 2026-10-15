package org.jmouse.script.el.parser;

import org.jmouse.script.el.StatementMatcher;
import org.jmouse.script.el.lexer.StatementToken;
import org.jmouse.core.Priority;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.node.ForNode;

/**
 * Parses {@code for entry in @store.pending('inbox') do … end}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(StatementPriority.LOOP)
public class LoopParser extends AbstractBodyParser<ForNode, StatementToken> {

    @Override
    protected ForNode createNode(TokenCursor cursor, ParserContext context) {
        ForNode node = new ForNode();

        node.setVariable(SourceReader.name(cursor));

        cursor.ensure(BasicToken.T_IN);

        node.setIterable(Expressions.read(cursor, context));

        cursor.ensure(StatementToken.T_DO);

        return node;
    }

    @Override
    protected StatementToken token() {
        return StatementToken.T_FOR;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return StatementMatcher.loop().matches(cursor);
    }

    @Override
    protected Class<? extends Parser> statementsParser() {
        return ScriptBodyParser.class;
    }

    @Override
    protected void closeBlock(TokenCursor cursor) {
        cursor.ensure(StatementToken.T_END);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        return (S) SourceReader.span(cursor);
    }

}
