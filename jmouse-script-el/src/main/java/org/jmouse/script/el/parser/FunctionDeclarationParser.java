package org.jmouse.script.el.parser;

import org.jmouse.script.el.StatementMatcher;
import org.jmouse.script.el.lexer.StatementToken;
import org.jmouse.core.Priority;
import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.type.TypeAnnotations;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.node.FunctionDeclarationNode;

/**
 * Parses {@code function overdue(entry) … end} and {@code function share(count : int, total : int) : double … end}.
 *
 * <p>{@code function} is a {@link LanguageToken} the engine already owns, so this dialect gets the
 * keyword without spelling it a second time — and a file that mixed a {@code jmouse-el} {@code function}
 * with a jMS one would be a file with two of them.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(StatementPriority.FUNCTION)
public class FunctionDeclarationParser extends AbstractBodyParser<FunctionDeclarationNode, LanguageToken> {

    @Override
    protected FunctionDeclarationNode createNode(TokenCursor cursor, ParserContext context) {
        FunctionDeclarationNode node = new FunctionDeclarationNode();

        node.setName(SourceReader.name(cursor));

        cursor.ensure(BasicToken.T_OPEN_PAREN);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            String parameter = SourceReader.name(cursor);
            TypeAnnotations.read(cursor, context).ifPresentOrElse(type -> node.addParameter(parameter, type),
                    () -> node.addParameter(parameter));

            if (!cursor.consumeIf(BasicToken.T_COMMA)) {
                break;
            }
        }

        cursor.ensure(BasicToken.T_CLOSE_PAREN);
        TypeAnnotations.read(cursor, context).ifPresent(node::setResultType);

        return node;
    }

    @Override
    protected LanguageToken token() {
        return LanguageToken.T_FUNCTION;
    }

    @Override
    protected boolean matches(TokenCursor cursor) {
        return StatementMatcher.function().matches(cursor);
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
