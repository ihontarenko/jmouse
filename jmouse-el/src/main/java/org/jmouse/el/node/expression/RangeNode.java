package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;

import java.util.Iterator;
import java.util.stream.IntStream;

public class RangeNode extends AbstractExpressionNode {

    private int               start;
    private int               end;
    private Iterator<Integer> iterator;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        if (iterator == null) {
            iterator = IntStream.range(start, end).boxed().iterator();
        }

        return (Iterable<Integer>) () -> RangeNode.this.iterator;
    }

    @Override
    public String toString() {
        return "%d .. %d".formatted(start, end);
    }

}
