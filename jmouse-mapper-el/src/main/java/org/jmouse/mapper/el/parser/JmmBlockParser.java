package org.jmouse.mapper.el.parser;

import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.node.expression.SpanNode;

/**
 * Base for every {@code { … }} block in the mapping language.
 *
 * <p>All it adds to {@link AbstractBodyParser} is a span pointing at the block's own keyword.</p>
 *
 * <p>⚠️ <strong>The span is not decoration.</strong> Everything that refuses a target later — a type
 * that does not resolve, an {@code unmapped fail} nothing can satisfy, a condition that will not
 * compile — runs in the binder, long after any cursor exists, and has only the node to name a line
 * with. Taken here, before the block's keyword is consumed, every block gets one without any parser
 * having to remember.</p>
 *
 * @param <N> the node this parser builds
 * @param <T> the token type that opens the block
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class JmmBlockParser<N extends ExpressionsNode, T extends Token.Type>
        extends AbstractBodyParser<N, T> {

    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        Token token = cursor.current();

        return (S) SpanNode.of(token.lineNumber(), SourceReading.column(cursor));
    }
}
