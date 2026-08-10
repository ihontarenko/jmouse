package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

abstract public class SpanNode extends AbstractExpression {

    private final int lineNumber;
    private final int position;

    public SpanNode(int lineNumber, int position) {
        this.lineNumber = lineNumber;
        this.position = position;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public int position() {
        return position;
    }

    @Override
    public String toString() {
        return "SpanNode[%d : %d]".formatted(lineNumber, position);
    }

    public static SpanNode of(int line, int position) {
        return new Simple(line, position);
    }

    public static class Simple extends SpanNode {

        public Simple(int lineNumber, int position) {
            super(lineNumber, position);
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return super.evaluate(context);
        }

    }

}
