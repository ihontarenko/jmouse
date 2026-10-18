package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopeValues;
import org.jmouse.el.type.ValueType;
import org.jmouse.script.el.Declarations;
import org.jmouse.script.el.budget.ScriptExecution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final List<String>           parameters     = new ArrayList<>();
    private final Map<String, ValueType> parameterTypes = new LinkedHashMap<>();

    private ValueType resultType;
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
     * Declares a typed parameter: {@code count : int}, which every argument given to it must be.
     *
     * @param parameter the name it is bound to inside the body
     * @param type      what an argument must be
     */
    public void addParameter(String parameter, ValueType type) {
        parameters.add(parameter);
        parameterTypes.put(parameter, type);
    }

    /** The type of a parameter; {@code null} for one written without a type. */
    public ValueType getParameterType(String parameter) {
        return parameterTypes.get(parameter);
    }

    /** {@code function f(x) : double} — what it returns must be; {@code null} when it does not say. */
    public ValueType getResultType() {
        return resultType;
    }

    public void setResultType(ValueType resultType) {
        this.resultType = resultType;
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
            admitArguments(context);
            return admitResult(context, evaluateBody(context));
        } catch (ReturnSignal signal) {
            return admitResult(context, signal.value());
        } finally {
            if (execution != null) {
                execution.leave();
            }
        }
    }

    /** What to call this function in a budget refusal. Built once; read on the counting path. */
    /**
     * Each typed parameter's argument as its type admits it, declared in the scope the call bound it in — so the body
     * cannot write a value of another type to it either.
     */
    private void admitArguments(EvaluationContext context) {
        if (parameterTypes.isEmpty()) {
            return;
        }
        ScopeValues scope = context.getScopedChain().peek();
        Declarations declarations = Declarations.of(context);
        parameterTypes.forEach((parameter, type) -> {
            Declarations.Declaration declaration = new Declarations.Declaration(parameter, type);
            scope.set(parameter, Declarations.admit(context, declaration, scope.get(parameter)));
            declarations.declare(scope, declaration);
        });
    }

    private Object admitResult(EvaluationContext context, Object result) {
        return resultType == null ? result
                : Declarations.admit(context, new Declarations.Declaration(name + "()", resultType), result);
    }

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
