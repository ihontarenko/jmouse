package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;

/**
 * One clause inside a {@code view} or {@code function} block — {@code where}, {@code order},
 * {@code columns}.
 *
 * <p>The keyword lives on the node rather than in the parser that built it, for two reasons. A block
 * refuses a repeated clause and needs to name it in the refusal; and {@code toSource()} has to write
 * the keyword back, so keeping it in one place is what stops the parsed spelling and the written
 * spelling drifting apart.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class ClauseNode extends AbstractExpression {

    /**
     * The word that opens this clause, as it is written.
     *
     * @return the clause keyword
     */
    public abstract String keyword();

    /**
     * The clause's own body, without the keyword.
     *
     * @return everything after the keyword, as source
     */
    protected abstract String bodyToSource();

    @Override
    public String toSource() {
        return "%s %s".formatted(keyword(), bodyToSource());
    }

    @Override
    public String toString() {
        return keyword();
    }
}
