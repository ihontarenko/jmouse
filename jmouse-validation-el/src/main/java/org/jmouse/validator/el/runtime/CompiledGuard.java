package org.jmouse.validator.el.runtime;

import org.jmouse.el.node.Expression;

import java.util.List;

/**
 * A {@code when … { … } otherwise { … }}, compiled.
 *
 * <h2>⚠️ Nesting needs no special case</h2>
 *
 * <p>A guard's body may hold another guard, and the inner one only ever runs because the outer one
 * chose to run it. That is what makes nesting conjunction, and it is why there is no code here
 * combining conditions: the tree does it by existing.</p>
 *
 * <h2>⚠️ The branch that did not run leaves its fields unasked, not passed</h2>
 *
 * <p>Every field named in the branch that was skipped is recorded as such, so the outcome can tell a
 * caller the difference. A field examined and found fine, and a field nobody looked at, are the same
 * absence of an error and very different answers.</p>
 *
 * @param condition the guard
 * @param body      what applies while it holds
 * @param otherwise what applies while it does not
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record CompiledGuard(Expression condition, List<CompiledItem> body, List<CompiledItem> otherwise)
        implements CompiledItem {

    public CompiledGuard {
        body = List.copyOf(body);
        otherwise = List.copyOf(otherwise);
    }

    @Override
    public void evaluate(RecordJudgement record) {
        boolean holds = record.holds(condition);

        record.evaluate(holds ? body : otherwise);
        record.skip(holds ? otherwise : body);
    }
}
