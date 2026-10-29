package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

/** {@code +- amount}: how far a value may stand from its mark — an expression, a share in {@code %} and all. */
public class ToleranceNode extends AbstractExpression {

    private final String      mark;
    private final FormulaNode amount;

    /** @param mark the sign as written: {@code ±}, or {@code +-} */
    public ToleranceNode(String mark, FormulaNode amount) {
        this.mark = mark;
        this.amount = amount;
    }

    public FormulaNode amount() {
        return amount;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return amount;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(mark).write(amount);
    }
}
