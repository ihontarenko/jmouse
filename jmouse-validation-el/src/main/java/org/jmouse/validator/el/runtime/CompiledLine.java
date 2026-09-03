package org.jmouse.validator.el.runtime;

import org.jmouse.el.node.Expression;
import org.jmouse.validator.constraint.api.Constraint;

import java.util.List;

/**
 * Everything asked of one field, compiled. 📏
 *
 * <h2>⚠️ Order is the mechanism, and {@code stop} is what makes it matter</h2>
 *
 * <p>Checks run left to right. A check written {@code stop} that fails ends <strong>this field's</strong>
 * list and nothing more — a blank part number should not also be told it fails a pattern, and a person
 * fixing the form should still learn what is wrong with every <em>other</em> field in the same
 * breath.</p>
 *
 * @param field   what the line is about
 * @param checks  what is asked, in order
 * @param message the message covering every check without one of its own
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record CompiledLine(String field, List<CompiledCheck> checks, Expression message)
        implements CompiledItem {

    public CompiledLine {
        checks = List.copyOf(checks);
    }

    @Override
    public void evaluate(RecordJudgement record) {
        Object value = record.field(field);

        for (CompiledCheck check : checks) {
            Constraint constraint = record.constraintOf(check, field, value);

            if (constraint.execute(value)) {
                continue;
            }

            record.refuse(field, wording(record, check, constraint));

            if (check.stop()) {
                return;
            }
        }
    }

    /**
     * What to say about a failed check: its own words, else the line's, else the constraint's.
     *
     * <p>⚠️ The last fallback is a code rather than a sentence — {@code size}, {@code min_max.RANGE} —
     * and it is deliberately unlovely. A document whose failures read like identifiers is a document
     * nobody wrote messages for, and that should be visible rather than papered over with a generated
     * sentence that reads as though somebody chose it.</p>
     *
     * @param record     the record being judged, for evaluating a message
     * @param check      the check that failed
     * @param constraint what it built
     * @return the wording
     */
    private String wording(RecordJudgement record, CompiledCheck check, Constraint constraint) {
        if (check.message() != null) {
            return record.say(check.message());
        }

        if (message != null) {
            return record.say(message);
        }

        return constraint.message() != null ? constraint.message() : field + " fails " + constraint.code();
    }
}
