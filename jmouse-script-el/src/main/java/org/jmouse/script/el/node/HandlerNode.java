package org.jmouse.script.el.node;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

/**
 * {@code on unload when building.kind == 'dropoff' do … end} — a body bound to a host event.
 *
 * <h2>⚠️ The event name is a string here, and only a string</h2>
 *
 * <p>The parser holds no list of events and cannot: which events exist is the host's to declare, and a
 * dialect that shipped a vocabulary would be a dialect with a game in it. Refusing {@code unlod} is the
 * binder's job, and it is done at load rather than the first time the handler happens not to fire.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class HandlerNode extends ScriptBodyNode {

    private String     described;
    private String     event;
    private Expression argument;
    private Expression condition;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * Returns the literal written after the event name — the {@code 180} of {@code on timer 180}.
     *
     * <p>What it means is the host's business: a period, a count, a channel. The language carries it
     * and does not interpret it.</p>
     *
     * @return the argument, or {@code null} where none was written
     */
    public Expression getArgument() {
        return argument;
    }

    public void setArgument(Expression argument) {
        this.argument = argument;
    }

    /**
     * Returns the {@code when} guard.
     *
     * @return the condition, or {@code null} for a handler that always runs
     */
    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    /**
     * Whether this handler's guard admits the event.
     *
     * <p>A handler with no {@code when} always matches, so the absent condition is not an empty
     * expression to evaluate — it is one branch that never touches the evaluator at all.</p>
     *
     * @param context the evaluation context, carrying whatever the host put in it for this event
     * @return {@code true} when the body should run
     */
    public boolean matches(EvaluationContext context) {
        if (condition == null) {
            return true;
        }

        Conversion conversion = context.getConversion();

        return Boolean.TRUE.equals(conversion.convert(condition.evaluate(context), Boolean.class));
    }

    /**
     * Runs the body, guard included.
     *
     * <p>⚠️ A {@link ReturnSignal} is caught here rather than allowed out: {@code return} inside a
     * handler means "this handler is finished", and a host dispatching an event should never have to
     * know that a control-flow exception exists.</p>
     *
     * @param context the evaluation context
     * @return what the body produced, or {@code null} when the guard refused it
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        if (!matches(context)) {
            return null;
        }

        try {
            return evaluateBody(context);
        } catch (ReturnSignal signal) {
            return signal.value();
        }
    }

    /**
     * What to call this handler in a budget refusal.
     *
     * <p>Built once and kept, because it is read on the counting path — once per statement.</p>
     */
    @Override
    protected String describe() {
        if (described == null) {
            described = "the handler for '" + event + "'";
        }

        return described;
    }

    @Override
    public String toString() {
        return "ON['%s']".formatted(event);
    }

}
