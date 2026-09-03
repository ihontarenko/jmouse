package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.lexer.ScriptToken;
import org.jmouse.script.el.node.BehaviourNode;

/**
 * Parses {@code behaviour "gatherer" do … end} — a named block of functions a host drives.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.BEHAVIOUR)
public class BehaviourParser extends AbstractBodyParser<BehaviourNode, ScriptToken> {

    @Override
    protected BehaviourNode createNode(TokenCursor cursor, ParserContext context) {
        BehaviourNode node = new BehaviourNode();

        node.setName(SourceReader.literal(cursor.ensure(BasicToken.T_STRING)));

        cursor.ensure(ScriptToken.T_DO);

        return node;
    }

    @Override
    protected ScriptToken token() {
        return ScriptToken.T_BEHAVIOUR;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.behaviour().matches(cursor);
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
