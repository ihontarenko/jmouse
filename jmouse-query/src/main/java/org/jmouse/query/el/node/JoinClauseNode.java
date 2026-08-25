package org.jmouse.query.el.node;

import org.jmouse.query.translate.Capability;

/**
 * {@code join: person on person.key == request.assignee} — a second structure, in the query.
 *
 * <h2>⚠️ This is not the {@code join:} a mapping writes</h2>
 *
 * <p>A mapping's join reaches another <em>table</em> in order to supply one structure's attribute, and
 * nobody writing a query ever sees it — an attribute simply resolves. A view's join combines two
 * <em>structures</em>, both named in the query, and both must be mapped wherever it runs.</p>
 *
 * <p>They share a word because they are the same idea at two levels, and the level is never in doubt: one
 * appears in a mapping and the other in a view.</p>
 *
 * <h2>⚠️ The condition is two attributes and an equality, not an expression</h2>
 *
 * <p>{@code on person.key == request.assignee} and nothing else. A join whose condition could be an
 * arbitrary expression is a join a backend cannot promise to honour — a row pipeline can hash two sides
 * on equal keys and cannot evaluate {@code a > b} without comparing every pair — and the language would
 * be offering something one destination silently degrades.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class JoinClauseNode extends ClauseNode {

    public static final ClauseKind KIND =
            ClauseKind.of("join", Capability.JOIN, ClauseKind.STEP / 2);

    public static final String KEYWORD = KIND.keyword();

    private String structure;
    private String left;
    private String right;

    /** The structure being joined in. */
    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    /** The attribute on the joined side — {@code person.key}. */
    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    /** The attribute on the side already being queried — {@code request.assignee}. */
    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }

    @Override
    public ClauseKind kind() {
        return KIND;
    }

    @Override
    protected String bodyToSource() {
        return "%s on %s == %s".formatted(structure, left, right);
    }
}
