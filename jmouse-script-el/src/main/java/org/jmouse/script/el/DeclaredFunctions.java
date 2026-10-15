package org.jmouse.script.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Lambda;
import org.jmouse.script.el.node.FunctionDeclarationNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A document's own functions, wrapped once as something an expression can call.
 *
 * <p>{@link org.jmouse.el.node.expression.FunctionNode} resolves a name it does not find among the registered
 * functions by looking for a {@link Lambda} in the evaluation context — which is exactly what a declared function
 * is: named parameters, a body, and a scope of its own while it runs. So {@code busy()} reaches
 * {@code function busy()} in the same file without a second dispatch mechanism, in every dialect.</p>
 *
 * <p>⚠️ <strong>Per context, not globally.</strong> Two documents loaded in one process routinely declare a
 * function of the same name, and they are not the same function; a registry shared by the whole expression
 * language would let the second quietly answer for the first.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class DeclaredFunctions {

    private final Map<String, Lambda> callables;

    private DeclaredFunctions(Map<String, Lambda> callables) {
        this.callables = callables;
    }

    /** Built once, at load — never on a path a host may take sixty times a second. */
    public static DeclaredFunctions of(Map<String, FunctionDeclarationNode> declarations) {
        Map<String, Lambda> callables = new LinkedHashMap<>();
        declarations.forEach((name, declaration) -> {
            Lambda callable = new Lambda(declaration);
            callable.setName(name);
            declaration.getParameters().forEach(callable::addParameter);
            callables.put(name, callable);
        });
        return new DeclaredFunctions(Map.copyOf(callables));
    }

    /** The callable a declared function was wrapped in, or {@code null}; a host calls it to run one directly. */
    public Lambda callable(String name) {
        return callables.get(name);
    }

    public Set<String> names() {
        return callables.keySet();
    }

    /** Makes the functions callable from the expressions evaluated in one context. */
    public void installInto(EvaluationContext context) {
        callables.forEach(context::setValue);
    }
}
