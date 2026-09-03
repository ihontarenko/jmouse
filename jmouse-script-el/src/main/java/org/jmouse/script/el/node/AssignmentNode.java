package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

/**
 * {@code entry.state = 'moving'} — a value written back where it was read from.
 *
 * <p>⚠️ <strong>The one statement in this language that is not opened by a keyword</strong>, and the
 * reason the grammar has it at all: a behaviour is a state machine written as {@code if} / {@code elseif}
 * over a field, and a state machine that cannot move to the next state is a list of conditions.</p>
 *
 * <p>What a path is allowed to reach is not this node's business. A script sees only what the host put
 * in the context for this event, so "may a script write {@code entry.state}" is answered by what the
 * host exposes there, not by the grammar.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AssignmentNode extends AbstractExpression {

    private String     path;
    private Expression value;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object evaluated = value.evaluate(context);

        context.setValue(path, evaluated);

        return evaluated;
    }

    @Override
    public String toSource() {
        return "%s = %s".formatted(path, value.toSource());
    }

    @Override
    public String toString() {
        return "ASSIGN['%s']".formatted(path);
    }

}
