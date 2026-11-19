package org.jmouse.java.parser.projection;

import org.jmouse.el.node.TraversalAction;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.model.LocalBinding;

import java.util.List;
import java.util.Objects;

/** Contributions from a statement; traversal remains the responsibility of the shared Visitor. */
public record JavaStatementProjection(
        List<Expression> expressions, List<LocalBinding> bindings, TraversalAction traversal) {
    public static final JavaStatementProjection TRAVERSE =
            new JavaStatementProjection(List.of(), List.of(), TraversalAction.CONTINUE);

    public JavaStatementProjection {
        expressions = List.copyOf(expressions);
        bindings = List.copyOf(bindings);
        Objects.requireNonNull(traversal, "traversal");
        if (traversal == TraversalAction.STOP) {
            throw new IllegalArgumentException(
                    "A navigation handler cannot silently truncate the rest of a body");
        }
    }
}
