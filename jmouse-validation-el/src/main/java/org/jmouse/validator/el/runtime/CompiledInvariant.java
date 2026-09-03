package org.jmouse.validator.el.runtime;

import org.jmouse.el.node.Expression;

/**
 * An assertion about the record — {@code invariant min_stock_threshold <= quantity : '…'}.
 *
 * @param condition what must hold
 * @param message   what to say when it does not
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record CompiledInvariant(Expression condition, Expression message) implements CompiledItem {

    @Override
    public void evaluate(RecordJudgement record) {
        if (record.holds(condition)) {
            return;
        }

        // ⚠️ Reported against no field. An invariant naming three of them is not a fact about whichever
        // was written first, and putting the error there would send somebody to fix the wrong one.
        record.refuse(null, record.say(message));
    }
}
