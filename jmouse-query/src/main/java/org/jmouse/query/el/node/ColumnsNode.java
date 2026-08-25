package org.jmouse.query.el.node;

import org.jmouse.el.node.Expression;
import org.jmouse.query.translate.Capability;
import org.jmouse.query.el.SourceWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code columns <expression> [as name] {, …}} — what to return.
 *
 * <p>⚠️ <strong>A projection is an expression, not a name</strong>, from the first version rather than
 * as a later widening. {@code columns price | decimal * 1.2 as retail} is what an export definition
 * actually needs, and a clause that took bare names would have had to grow a second syntax to allow it
 * — by which time documents exist that the new syntax has to stay compatible with.</p>
 *
 * <p>A bare name is simply the degenerate case: an expression that happens to be one identifier.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ColumnsNode extends ClauseNode {

    public static final ClauseKind KIND =
            ClauseKind.of("fetch", Capability.PROJECT, 1 * ClauseKind.STEP);

    public static final String KEYWORD = KIND.keyword();

    private final List<Projection> projections = new ArrayList<>();

    public void addProjection(Expression expression, String alias) {
        projections.add(new Projection(expression, alias));
    }

    public List<Projection> getProjections() {
        return List.copyOf(projections);
    }

    @Override
    public ClauseKind kind() {
        return KIND;
    }

    @Override
    protected String bodyToSource() {
        return projections.stream().map(Projection::toSource).collect(Collectors.joining(", "));
    }

    /**
     * One returned value, and what to call it.
     *
     * @param expression what to return
     * @param alias      the name it is returned under, or {@code null} when none was written
     */
    public record Projection(Expression expression, String alias) {

        public String toSource() {
            String rendered = expression.toSource();

            return alias == null ? rendered : "%s as %s".formatted(rendered, SourceWriter.name(alias));
        }
    }
}
