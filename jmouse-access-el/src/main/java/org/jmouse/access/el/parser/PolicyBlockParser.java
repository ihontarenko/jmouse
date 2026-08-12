package org.jmouse.access.el.parser;

import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.lexer.AccessToken;
import org.jmouse.el.language.parser.AbstractBodyParser;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.expression.ExpressionsNode;
import org.jmouse.el.node.expression.SpanNode;

/**
 * Base for every {@code { … }} block in the policy language.
 *
 * <p>The one thing it changes is the span: the framework's default carries a position that cannot be
 * turned into a record, and a policy node's span is read back by stage 2 to name a line. Producing a
 * {@link org.jmouse.access.el.node.SourceSpanNode} here means every block gets a usable one without
 * having to remember to set it, and gets it pointing at the keyword — the span is taken before the
 * block's own token is consumed.</p>
 *
 * <p>The name is not {@code AbstractParser}: the framework has one of those, half this package
 * extends it, and two different classes answering to one name in the same import list is a reader
 * having to check which is which every time.</p>
 *
 * @param <N> the node this parser builds
 * @param <T> the token type that opens the block
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class PolicyBlockParser<N extends ExpressionsNode, T extends Token.Type>
        extends AbstractBodyParser<N, T> {

    /**
     * Takes the block's span — and first swallows the optional {@code declare} / {@code assign}
     * prefix.
     *
     * <p>⚠️ <strong>Here and nowhere else.</strong> Those two words say what <em>kind</em> of statement
     * a block is — structure, or a case — and every block may take one, so the single place that
     * already knows "this is a policy block" is the single place that should know how one opens. The
     * same {@code if} repeated across nine parsers is the shape where the tenth is forgotten and one
     * block quietly stops accepting the canonical spelling.
     *
     * <p>⚠️ <strong>It happens in {@code span} because {@code parse} is final</strong>, and that turns
     * out to be the right seam rather than a workaround: {@code AbstractBlockParser} calls this
     * <em>before</em> {@code ensure(token())}, so consuming the prefix here leaves the cursor exactly
     * where the base class expects it and leaves the span pointing at the block's own keyword — which
     * is what a reported line number has to mean.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <S extends SpanNode> S span(TokenCursor cursor) {
        cursor.consumeIf(AccessToken.T_DECLARE, AccessToken.T_ASSIGN);

        return (S) SourceReader.span(cursor);
    }
}
