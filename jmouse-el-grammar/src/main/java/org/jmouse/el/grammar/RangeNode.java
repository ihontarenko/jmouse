package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

import java.util.Optional;

/**
 * The values something may take: {@code low .. high}, each an expression, or a list in brackets — {@code [1, 2, 4]},
 * {@code ["write", "read"]} —, kept as written.
 */
public class RangeNode extends AbstractExpression {

    private final FormulaNode low;
    private final FormulaNode high;
    private final FormulaNode listed;

    private RangeNode(FormulaNode low, FormulaNode high, FormulaNode listed) {
        this.low = low;
        this.high = high;
        this.listed = listed;
    }

    public static RangeNode between(FormulaNode low, FormulaNode high) {
        return new RangeNode(low, high, null);
    }

    public static RangeNode listing(FormulaNode listed) {
        return new RangeNode(null, null, listed);
    }

    public Optional<FormulaNode> low() {
        return Optional.ofNullable(low);
    }

    public Optional<FormulaNode> high() {
        return Optional.ofNullable(high);
    }

    /** The list in brackets, when the range is one. */
    public Optional<FormulaNode> listed() {
        return Optional.ofNullable(listed);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        if (listed != null) {
            writer.write(listed);
        } else {
            writer.write(low).word("..").write(high);
        }
    }
}
