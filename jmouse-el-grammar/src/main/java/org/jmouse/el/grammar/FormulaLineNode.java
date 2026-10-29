package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

/**
 * {@code left = right}: a formula's definition, an equation, a heat law's line — both sides as written, for jMF. What the
 * left side says — a derivative, a branch, a {@code let} — is the module's to tell, by the shape of what it holds.
 */
public class FormulaLineNode extends AbstractExpression {

    private final FormulaNode left;
    private final FormulaNode right;
    private final FormulaNode statement;

    /** @param statement the whole line as its author wrote it — one statement of jMF, read as one */
    public FormulaLineNode(FormulaNode left, FormulaNode right, FormulaNode statement) {
        this.left = left;
        this.right = right;
        this.statement = statement;
    }

    /** The whole line as written: {@code delay_scale(supply) = (nominal_supply / supply) ^ delay_exponent}. */
    public FormulaNode statement() {
        return statement;
    }

    public FormulaNode left() {
        return left;
    }

    public FormulaNode right() {
        return right;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.write(left).word("=").write(right);
    }

    @Override
    public String toString() {
        return left + " = " + right;
    }
}
