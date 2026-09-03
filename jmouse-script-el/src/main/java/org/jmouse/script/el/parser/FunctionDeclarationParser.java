package org.jmouse.script.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.CursorMatcher;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.lexer.ScriptToken;
import org.jmouse.script.el.node.FunctionDeclarationNode;

/**
 * Parses {@code function overdue(entry) … end}.
 *
 * <p>{@code function} is a {@link LanguageToken} the engine already owns, so this dialect gets the
 * keyword without spelling it a second time — and a file that mixed a {@code jmouse-el} {@code function}
 * with a jMS one would be a file with two of them.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(ParserPriority.FUNCTION)
public class FunctionDeclarationParser extends AbstractBodyParser<FunctionDeclarationNode, LanguageToken> {

    @Override
    protected FunctionDeclarationNode createNode(TokenCursor cursor, ParserContext context) {
        FunctionDeclarationNode node = new FunctionDeclarationNode();

        node.setName(SourceReader.name(cursor));

        cursor.ensure(BasicToken.T_OPEN_PAREN);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            node.addParameter(SourceReader.name(cursor));

            if (!cursor.consumeIf(BasicToken.T_COMMA)) {
                break;
            }
        }

        cursor.ensure(BasicToken.T_CLOSE_PAREN);

        return node;
    }

    @Override
    protected LanguageToken token() {
        return LanguageToken.T_FUNCTION;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return CursorMatcher.function().matches(cursor);
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
