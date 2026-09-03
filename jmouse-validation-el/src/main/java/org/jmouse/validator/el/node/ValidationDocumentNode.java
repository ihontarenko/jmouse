package org.jmouse.validator.el.node;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.List;
import java.util.Optional;

/**
 * One {@code .jmv} file: {@code validation "innoventa/part" { … }}.
 *
 * <p>The name is the document's identity — what a product asks for when it wants this set of checks —
 * and it is quoted for the same reason a {@code .jmp} policy's and a {@code .jmm} mapping's are: it is
 * an address with slashes in it, not an identifier.</p>
 *
 * <p>⚠️ The gate is <strong>found</strong> among the statements rather than held in a field of its own.
 * A field would have meant the document parser reaching into the body to hoist it, which is the one
 * thing {@link org.jmouse.el.language.parser.AbstractBodyParser} exists to stop every language doing
 * differently. That the gate runs first is the runtime's rule, and the runtime asks for it by name.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ValidationDocumentNode extends ExpressionsNode {

    private String name;

    /** @return what this document is called — {@code innoventa/part} */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The block that decides whether the rest of the document is worth evaluating.
     *
     * @return the gate, or empty when the file has none
     */
    public Optional<CheckBlockNode> getGate() {
        return blocksOf(CheckBlockNode.Kind.GATE).stream().findFirst();
    }

    /**
     * Every block of one kind, in the order they were written.
     *
     * @param kind which block to look for
     * @return the blocks; never {@code null}
     */
    public List<CheckBlockNode> blocksOf(CheckBlockNode.Kind kind) {
        return getExpressions(CheckBlockNode.class).stream()
                .map(CheckBlockNode.class::cast)
                .filter(block -> block.getKind() == kind)
                .toList();
    }

    /**
     * Everything the document asks for, in the order it was written.
     *
     * @return the statements — check lines, invariants, {@code when}s and {@code always} blocks
     */
    public List<Expression> getBody() {
        return getExpressions();
    }

    @Override
    public String toString() {
        return "validation \"" + name + "\"";
    }
}
