package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.el.evaluation.ScopeValues;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.node.Expression;
import org.jmouse.script.el.budget.ScriptExecution;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * {@code for entry in @store.pending('inbox') do … end}
 *
 * <p>One shape only. There is no C-style counting loop, because everything a script iterates comes from
 * a facade as a collection, and a second loop syntax would exist purely so somebody could write an
 * index nobody needed.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ForNode extends ScriptBodyNode {

    private String     variable;
    private String     described;
    private Expression iterable;

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public Expression getIterable() {
        return iterable;
    }

    public void setIterable(Expression iterable) {
        this.iterable = iterable;
    }

    /**
     * Walks the collection, binding each element to the loop variable and running the body.
     *
     * <p>⚠️ <strong>Nothing to iterate is not a failure.</strong> A facade answering {@code null} —
     * no units in that area, no offers on that part — is the ordinary case, and a loop that threw over
     * it would make every script defensive about a question it already asked.</p>
     *
     * <p>⚠️ <strong>But something that is not a collection is.</strong> A facade answering a number
     * where a list was expected is a mistake in the script or a changed facade, and skipping the loop
     * silently is the shape where a script stops doing anything on minute forty with nothing anywhere
     * to say why. {@code null} is an answer; a string is not.</p>
     *
     * <p>⚠️ <strong>The loop variable lives in a scope of its own.</strong> Written straight into the
     * enclosing one it would outlive the loop and — worse — overwrite whatever the host had put in the
     * context under that name, which is exactly what happens when somebody names a loop variable after
     * the event's own subject.</p>
     *
     * @param context the evaluation context
     * @return {@code null}; a loop is run for what its body does
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Object evaluated = iterable.evaluate(context);

        if (evaluated == null) {
            return null;
        }

        Iterator<?> elements = elements(evaluated);

        ScriptExecution execution = ScriptExecution.from(context);
        ScopedChain     chain     = context.getScopedChain();
        ScopeValues     scope     = chain.push();
        long            taken     = 0;

        try {
            while (elements.hasNext()) {
                // ⚠️ Counted here rather than left to the body's own step: a loop with an empty body
                // still goes round, and going round is the thing that has to be bounded.
                if (execution != null) {
                    execution.iteration(++taken, variable);
                    execution.step(describe());
                }

                scope.set(variable, elements.next());
                evaluateBody(context);
            }
        } finally {
            chain.pop();
        }

        return null;
    }

    /**
     * Turns whatever a facade answered into something to walk.
     *
     * <p>⚠️ <strong>Read directly rather than through the conversion service.</strong> Asking it for an
     * {@code Iterator} looks tidier and does not work: the registered converter is declared
     * {@code Iterable → Iterator}, the lookup matches on the concrete class, and an ordinary
     * {@code List} therefore comes back as <em>"no registered converter found"</em> — every loop over
     * every list in the language, failing on a sentence about converters that says nothing about the
     * script. Three type checks are both faster and honest about what a loop accepts.</p>
     *
     * @param evaluated what the expression answered; never {@code null} here
     * @return an iterator over it
     * @throws EvaluationException when it is not something a loop can walk
     */
    private Iterator<?> elements(Object evaluated) {
        return switch (evaluated) {
            case Iterable<?> iterable -> iterable.iterator();
            case Iterator<?> iterator -> iterator;
            case Map<?, ?> map -> map.entrySet().iterator();
            case Object[] array -> Arrays.asList(array).iterator();
            default -> throw new EvaluationException(
                    ("'for %s in …' needs something to walk — a list, a set, a map or an array — and this "
                            + "answered a %s")
                            .formatted(variable, evaluated.getClass().getSimpleName()));
        };
    }

    /** What to call this loop in a budget refusal. Built once; read on the counting path. */
    @Override
    protected String describe() {
        if (described == null) {
            described = "the loop over '" + variable + "'";
        }

        return described;
    }

    @Override
    public String toString() {
        return "FOR['%s' in %s]".formatted(variable, iterable);
    }

}
