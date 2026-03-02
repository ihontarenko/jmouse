package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.KeyValueNode;
import org.jmouse.validator.constraint.model.ValidationDefinition;

import java.util.Map;

/**
 * AST node representing a validation definition expression (e.g. {@code @MinMax(...)}). 🧱
 *
 * <p>
 * This node is produced by {@link ValidatorDefinitionParser}.
 * During evaluation it constructs a {@link ValidationDefinition}
 * using:
 * </p>
 * <ul>
 *     <li>the constraint name (identifier after '{@code @}'),</li>
 *     <li>evaluated key-value pairs inside parentheses.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @MinMax('min':3, 'mode':'MIN', 'message':'out of range')
 * }</pre>
 *
 * <p>
 * After evaluation, this node produces:
 * </p>
 *
 * <pre>{@code
 * ValidationDefinition {
 *     name = "MinMax",
 *     properties = {
 *         "min" -> 3,
 *         "mode" -> "MIN",
 *         "message" -> "out of range"
 *     }
 * }
 * }</pre>
 *
 * <p>
 * The resulting {@link ValidationDefinition} is later converted
 * into a concrete {@link org.jmouse.validator.constraint.api.Constraint}
 * by {@link ConstraintExpressionAdapter}.
 * </p>
 */
public class ValidationDefinitionNode extends AbstractExpression {

    private final String name;

    /**
     * Creates a new validation definition node.
     *
     * @param name constraint identifier (e.g. "MinMax", "OneOf")
     */
    public ValidationDefinitionNode(String name) {
        this.name = name;
    }

    /**
     * @return constraint identifier extracted from the EL expression
     */
    public String getName() {
        return name;
    }

    /**
     * Evaluates this node into a {@link ValidationDefinition}.
     *
     * <p>
     * Each {@link KeyValueNode} child is evaluated into a {@link Map.Entry},
     * whose key/value are inserted into the definition.
     * </p>
     *
     * @param context EL evaluation context
     * @return populated {@link ValidationDefinition}
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        ValidationDefinition definition = new ValidationDefinition(getName());

        for (Node child : getChildren()) {
            if (child instanceof KeyValueNode keyValue) {
                Object evaluated = keyValue.evaluate(context);

                if (evaluated instanceof Map.Entry<?, ?> entry) {
                    definition.put((String) entry.getKey(), entry.getValue());
                }
            }
        }

        return definition;
    }

}