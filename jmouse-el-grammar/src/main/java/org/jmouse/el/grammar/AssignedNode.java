package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.writer.SourceWriter;

/** {@code = value}: what a name is given — an expression ({@link FormulaNode}), or a text in quotes ({@link WordNode}). */
public class AssignedNode extends AbstractExpression {

    private final Expression value;

    public AssignedNode(Expression value) {
        this.value = value;
    }

    public Expression value() {
        return value;
    }

    /** Whether it is a text in quotes rather than an expression. */
    public boolean isText() {
        return value instanceof WordNode;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return value;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word("=").write(value);
    }
}
