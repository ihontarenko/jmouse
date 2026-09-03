package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.lexer.ScriptToken;
import org.jmouse.script.el.node.ScriptNode;

/**
 * Parses {@code script "slice-01" { … }} — a named block of handlers and the functions they call.
 *
 * <p>The body is read by the engine's brace-delimited reader, unchanged. A file-scope wrapper holds
 * declarations rather than statements, and a brace is what this engine's other languages already use
 * to say so.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.SCRIPT)
public class ScriptParser extends AbstractBodyParser<ScriptNode, ScriptToken> {

    @Override
    protected ScriptNode createNode(TokenCursor cursor, ParserContext context) {
        ScriptNode node = new ScriptNode();

        node.setName(SourceReader.literal(cursor.ensure(BasicToken.T_STRING)));

        return node;
    }

    @Override
    protected ScriptToken token() {
        return ScriptToken.T_SCRIPT;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.script().matches(cursor);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        return (S) SourceReader.span(cursor);
    }

}
