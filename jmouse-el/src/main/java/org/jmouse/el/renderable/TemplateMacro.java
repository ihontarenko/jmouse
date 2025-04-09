package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.renderable.node.MacroNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;

/**
 * Represents a template macro, which binds a macro name to its corresponding macro definition.
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
        context.getScopedChain().push();

        ExpressionNode parameters = node.getArguments();

        if (parameters != null && parameters.evaluate(context) instanceof Object[] evaluated) {
            List<Object>        values    = Arrays.asList(evaluated);
            List<String>        keys      = node().getArguments();
            Map<String, Object> arguments = range(0, values.size()).boxed().collect(toMap(keys::get, values::get));
            arguments.forEach(context::setValue);
        }

        node().getBody().accept(visitor);
        context.getScopedChain().pop();
    }

}
