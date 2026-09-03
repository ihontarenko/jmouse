package org.jmouse.validator.el.node;

import org.jmouse.el.node.AbstractExpression;

/**
 * An assertion about the record rather than about one field —
 * {@code invariant min_stock_threshold <= quantity : 'Above stock'}.
 *
 * <h2>⚠️ Why it is not simply a check on one of the fields it names</h2>
 *
 * <p>It has no field to belong to. Attaching it to whichever field it mentions first would put the
 * error on that one, which is a coin toss dressed up as an answer: a threshold above a quantity is not
 * a fact about the threshold, and telling somebody to fix that field is as likely to be wrong as
 * right.</p>
 *
 * <p>So an invariant reports against the record, and whatever renders errors has to have somewhere to
 * put one that names no field. That is a real cost, paid deliberately, and it is the honest shape.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class InvariantNode extends AbstractExpression {

    private String condition;
    private String message;

    /** @return what must hold, as it was written */
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    /** @return what to say when it does not hold, as an expression */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "invariant " + condition;
    }
}
