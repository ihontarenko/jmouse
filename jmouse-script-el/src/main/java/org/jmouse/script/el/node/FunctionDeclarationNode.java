package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.script.el.budget.ScriptExecution;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code function overdue(entry) … end} — a named body a script may call by name.
 *
 * <p>Named for the declaration rather than for the call, because
 * {@link org.jmouse.el.node.expression.FunctionNode} already means <em>a call</em> in this engine and
 * two different things answering to one name in an import list is a reader checking which is which
 * every time.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FunctionDeclarationNode extends ScriptBodyNode {

    private final List<String> parameters = new ArrayList<>();

    private String name;
    private String described;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the parameter names, in the order they were declared.
     *
     * @return the parameter names
     */
    public List<String> getParameters() {
        return List.copyOf(parameters);
    }

    /**
     * Declares a parameter.
     *
     * @param parameter the name it is bound to inside the body
     */
    public void addParameter(String parameter) {
        parameters.add(parameter);
    }

    /**
     * Runs the body, with whatever the caller has already bound the parameters to.
     *
     * <p>⚠️ <strong>This does not bind arguments</strong> — binding them means opening a scope, and who
     * owns that scope is the host SPI's question rather than a node's. A caller invokes this with the
     * parameters already in the context.</p>
     *
     * <p>⚠️ <strong>This is where recursion depth is counted</strong>, because it is the only place a
     * script-declared function is entered — whether a host invoked it by name or an expression reached
     * it through the {@code Lambda} the bound script installed. A function that calls itself has no
     * other guard: a step budget catches it eventually, and only after the stack is already deep.</p>
     *
     * <p>⚠️ The matching {@code leave()} is in a {@code finally}. A function that returns through a
     * {@code return} leaves this method by exception, and a depth left raised would refuse the next
     * handler in the same dispatch for something it did not do.</p>
     *
     * @param context the evaluation context
     * @return what the function returned, or the value of its last statement
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        ScriptExecution execution = ScriptExecution.from(context);

        if (execution != null) {
            execution.enter(name);
        }

        try {
            return evaluateBody(context);
        } catch (ReturnSignal signal) {
            return signal.value();
        } finally {
            if (execution != null) {
                execution.leave();
            }
        }
    }

    /** What to call this function in a budget refusal. Built once; read on the counting path. */
    @Override
    protected String describe() {
        if (described == null) {
            described = "the function '" + name + "'";
        }

        return described;
    }

    @Override
    public String toString() {
        return "FUNCTION['%s'(%s)]".formatted(name, String.join(", ", parameters));
    }

}
