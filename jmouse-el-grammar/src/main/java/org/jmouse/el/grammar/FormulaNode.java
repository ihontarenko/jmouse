package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceWriter;

/**
 * An expression as its author wrote it — {@code 100 * abs(anode.current) / I_maximum}, {@code 4.7 kΩ} —, kept for jMF,
 * whose own grammar reads it where it is computed. The document says where it stands and what it is for; what it means
 * is the formula language's.
 */
public class FormulaNode extends AbstractExpression {

    private final String written;

    public FormulaNode(String written) {
        this.written = written;
    }

    public String written() {
        return written;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return written;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(written);
    }

    @Override
    public String toString() {
        return written;
    }
}
