package org.jmouse.formula.el;

import org.jmouse.el.node.Expression;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** One statement of a formula document, read by {@link FormulaDocumentParser}. */
sealed interface FormulaStatement {

    /** Where the statement starts in the source, for a refusal to point at. */
    int offset();

    /**
     * {@code left = right} at the top level: a definition when another statement follows it, the document's equation
     * when it is the last.
     *
     * @param head  the name and parameters, when the left side is shaped as a definition's: {@code r},
     *              {@code f(u, v)}
     * @param left  the left side, as an expression and as written
     * @param right the right side, as an expression and as written
     */
    record Assignment(Optional<Head> head, Side left, Side right, int offset) implements FormulaStatement {
    }

    /** An expression with no top-level {@code =}: only the last statement may be one. */
    record Plain(Side expression, int offset) implements FormulaStatement {
    }

    /** {@code name} or {@code name(parameter, …)}: what a definition defines. */
    record Head(String name, List<String> parameters) {
    }

    /** A side of a statement: what jME parsed, the source it was cut from, and the names written in it. */
    record Side(Expression expression, String text, Set<String> names) {
    }
}
