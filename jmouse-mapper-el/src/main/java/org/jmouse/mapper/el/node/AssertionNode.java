package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One line of a {@code refuse} block: {@code condition : "message"}.
 *
 * <p>⚠️ The condition is what makes the mapping <strong>stop</strong>, not what must hold. The block is
 * called {@code refuse} so that the line reads the way it runs — {@code refuse … status is null :
 * "source order is corrupted"} — with no negation to hold in the reader's head and none to write, which
 * matters because the message and the condition would otherwise point in opposite directions.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AssertionNode extends AbstractExpression {

    private String condition;
    private String message;

    /** @return the condition, exactly as typed; when it holds, the mapping is refused */
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * @return what to report. It says what is wrong with the data, never what the code should have
     *         done
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "%s : \"%s\"".formatted(condition, message);
    }
}
