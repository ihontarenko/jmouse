package org.jmouse.query.el.node;

import org.jmouse.el.translate.Capability;

/**
 * {@code limit: 50} — at most this many rows.
 *
 * <h2>⚠️ The first clause added after the language was opened, and that is the point of it</h2>
 *
 * <p>It costs a parser and a capability. Not an edit to {@link QueryBlockNode}, not an edit to
 * {@code requireSupport}, not an edit to a single translator — the clause carries its own
 * {@link ClauseKind} and everything reads it from there. A clause that took more than that would mean the
 * openness was a claim rather than a property.</p>
 *
 * <h2>⚠️ It is a count, not an expression</h2>
 *
 * <p>{@code limit: someAttribute} would be a query whose size depends on a row, which no backend can
 * honour and every backend would honour differently. A bound value is a different question and is not
 * this clause.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class LimitNode extends ClauseNode {

    public static final ClauseKind KIND =
            ClauseKind.of("limit", Capability.LIMIT, 6 * ClauseKind.STEP);

    public static final String KEYWORD = KIND.keyword();

    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public ClauseKind kind() {
        return KIND;
    }

    @Override
    protected String bodyToSource() {
        return Integer.toString(count);
    }
}
