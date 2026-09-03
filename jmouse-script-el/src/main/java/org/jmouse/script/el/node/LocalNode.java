package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

/**
 * {@code local slot = @store.next_slot(entry)} — a name bound for the rest of a body.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class LocalNode extends AbstractExpression {

    private String     name;
    private Expression value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    /**
     * Binds the name in the <strong>current</strong> scope — the body's own.
     *
     * <p>⚠️ <strong>Not {@code context.setValue}</strong>, and the difference is the whole meaning of
     * the keyword. {@code setValue} walks the chain looking for a scope that <em>already</em> holds the
     * name and writes there, so a {@code local} would quietly overwrite an outer variable of the same
     * name rather than shadowing it — the one thing a reader of {@code local} is entitled to assume
     * cannot happen. Writing into {@link org.jmouse.el.evaluation.ScopedChain#peek()} shadows;
     * {@link ScriptBodyNode} is what makes sure there is a scope of this body's own to shadow in.</p>
     *
     * @param context the evaluation context
     * @return the value bound
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Object evaluated = value.evaluate(context);

        context.getScopedChain().peek().set(name, evaluated);

        return evaluated;
    }

    @Override
    public String toSource() {
        return "local %s = %s".formatted(name, value.toSource());
    }

    @Override
    public String toString() {
        return "LOCAL['%s']".formatted(name);
    }

}
