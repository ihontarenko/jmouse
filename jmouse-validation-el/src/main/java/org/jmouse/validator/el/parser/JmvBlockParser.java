package org.jmouse.validator.el.parser;

import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.node.expression.SpanNode;

/**
 * Base for every {@code { … }} block in the validation language.
 *
 * <p>All it adds to {@link AbstractBodyParser} is a span pointing at the block's own keyword, so every
 * block gets a usable line number without each parser having to remember to set one — and gets it
 * pointing at the word somebody wrote rather than at wherever the cursor happened to end up.</p>
 *
 * <p>The name is not {@code AbstractBlockParser}: the framework has one of those, this extends it
 * through {@code AbstractBodyParser}, and two classes answering to one name in an import list is a
 * reader checking which is which every time.</p>
 *
 * @param <N> the node this parser builds
 * @param <T> the token type that opens the block
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class JmvBlockParser<N extends ExpressionsNode, T extends Token.Type>
        extends AbstractBodyParser<N, T> {

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        Token token = cursor.current();

        return (S) SpanNode.of(token.lineNumber(), token.offset());
    }
}
