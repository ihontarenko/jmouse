package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.renderable.node.MacroNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Represents a view macro, which binds a macro name to its corresponding macro definition.
 * <p>
 * This record implements the {@link Macro} interface and encapsulates an immutable mapping
 * between a macro name and a {@link MacroNode} that contains the macro's content.
 * </p>
 *
 * @param name the name of the macro
 * @param node the {@link MacroNode} representing the macro's definition and body
 */
public record TemplateMacro(String name, MacroNode node, String source) implements Macro {

    @Override
    public void evaluate(Visitor visitor, FunctionNode node, EvaluationContext context) {
        // Retrieve the arguments expression from the function node invocation.
        Expression parameters = node.getArguments();
        MacroNode  macro      = node();
        // Get the current scope chain.
        ScopedChain chain = context.getScopedChain();

        // Push a new scope for macro execution.
        chain.push();

        // If arguments are provided, evaluate them and merge with the macro's expected parameters.
        if (parameters != null && parameters.evaluate(context) instanceof Object[] evaluated) {

            List<Object>        values    = Arrays.asList(evaluated);
            List<String>        keys      = macro.getArguments();
            Map<String, Object> arguments = new HashMap<>();

            // Match each expected parameter with the provided value or a default, if available.
            for (int i = 0, size = keys.size(); i < size; i++) {
                String key = keys.get(i);
                if (values.size() > i) {
                    arguments.put(key, values.get(i));
                } else if (macro.getDefaultValue(key) instanceof Expression defaultValue) {
                    arguments.put(key, defaultValue.evaluate(context));
                } else {
                    arguments.put(key, null);
                }
            }

            // Set all argument values in the current evaluation context.
            arguments.forEach(context::setValue);
        }

        // Process the macro body using the provided visitor.
        macro.getBody().accept(visitor);

        // Pop the macro scope to restore the previous context.
        chain.pop();
    }

}
