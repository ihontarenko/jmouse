package org.jmouse.el.grammar;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.writer.SourceWriter;

import java.util.List;

/** Values between commas — {@code vcc, 0}, {@code 0.25, 0.4} — or spaces — {@code 1 7} —, each a node that writes itself. */
public class ListNode extends AbstractExpression {

    private final List<Expression> items;
    private final boolean          commas;

    public ListNode(List<? extends Expression> items) {
        this(items, true);
    }

    /** @param commas whether the values stand between commas, rather than spaces */
    public ListNode(List<? extends Expression> items, boolean commas) {
        this.items = List.copyOf(items);
        this.commas = commas;
    }

    public List<Expression> items() {
        return items;
    }

    /** Whether its values stand between commas, rather than spaces. */
    public boolean commas() {
        return commas;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return items;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        if (commas) {
            writer.each(items, ",");
        } else {
            writer.each(items);
        }
    }
}
