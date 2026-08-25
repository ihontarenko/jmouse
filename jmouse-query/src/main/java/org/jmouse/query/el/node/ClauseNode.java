package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.QueryParseException;

/**
 * One clause of a block — {@code where}, {@code order}, {@code fetch}, and whatever is added next.
 *
 * <p>A clause knows three things about itself and nothing about the block it sits in: what it is
 * ({@link #kind()}), how its body reads ({@link #bodyToSource()}), and — when it may be written more
 * than once — how two of them combine ({@link #merge(ClauseNode)}).</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class ClauseNode extends AbstractExpression {

    /**
     * What this clause is — its keyword, the capability a backend needs, where it renders, and whether
     * it may be repeated. See {@link ClauseKind} for why this is carried rather than tabulated.
     */
    public abstract ClauseKind kind();

    /** The word that opens it. */
    public String keyword() {
        return kind().keyword();
    }

    /**
     * Combines another clause of the same kind into this one.
     *
     * <p>⚠️ Only ever called for a clause whose repetition is {@link ClauseKind.Repetition#MERGED}, and
     * the default refuses rather than quietly keeping one of the two. A clause that declares itself
     * merged without overriding this would lose whichever half the block happened to discard — which is
     * the silent-wrongness this whole area is built to avoid. A {@link ClauseKind.Repetition#MANY}
     * clause never reaches here at all: its repeats stay apart.</p>
     *
     * @param other the second one, of the same kind
     */
    public void merge(ClauseNode other) {
        throw new QueryParseException(
                "'%s' says it may be written twice but does not say how two of them combine"
                        .formatted(keyword()));
    }

    /** What follows the keyword. */
    protected abstract String bodyToSource();

    /**
     * ⚠️ {@code keyword: body} — the colon is written, always.
     *
     * <p>A view's keys are the language's own words, so the colon collides with nothing and the block
     * reads the way the structure and mapping blocks beside it do. The READER still accepts a clause
     * without one, which is what keeps every stored query working; nothing writes one.</p>
     */
    @Override
    public String toSource() {
        return "%s: %s".formatted(keyword(), bodyToSource());
    }

    @Override
    public String toString() {
        return keyword();
    }
}
