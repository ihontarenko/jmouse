package org.jmouse.action.adapter.el;

import org.jmouse.action.ActionDefinition;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.KeyValueNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AST node representing an action definition declared in EL. ⚙️
 *
 * <p>
 * This node stores:
 * </p>
 * <ul>
 *     <li>action name</li>
 *     <li>raw argument child nodes, usually {@link KeyValueNode}</li>
 * </ul>
 *
 * <p>
 * During evaluation, all child key-value nodes are evaluated and collected into
 * an ordered argument map, which is then used to create an {@link ActionDefinition}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @Action[autoload]{'source':'user','mode':'lazy'}
 * }</pre>
 *
 * <p>
 * After evaluation, this node produces:
 * </p>
 *
 * <pre>{@code
 * new ActionDefinition.Default("autoload", Map.of(
 *     "source", "user",
 *     "mode", "lazy"
 * ))
 * }</pre>
 */
public class ActionDefinitionNode extends AbstractExpression {

    /**
     * Action name declared in the expression.
     */
    private String name;

    /**
     * Optional preconfigured arguments map.
     *
     * <p>
     * In normal parsing flow, arguments are usually derived from child nodes
     * during {@link #evaluate(EvaluationContext)}.
     * </p>
     */
    private Map<String, Object> arguments;

    /**
     * Creates an action definition node for the given action name.
     *
     * @param name action name
     */
    public ActionDefinitionNode(String name) {
        this.name = name;
    }

    /**
     * Returns the action name.
     *
     * @return action name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the action name.
     *
     * @param name action name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns configured arguments.
     *
     * @return arguments map, may be {@code null}
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Sets configured arguments.
     *
     * @param arguments arguments map
     */
    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    /**
     * Evaluates this node into an {@link ActionDefinition}.
     *
     * <p>
     * Each child {@link KeyValueNode} is evaluated and converted into a map entry.
     * All resolved entries are collected into a {@link LinkedHashMap} to preserve
     * declaration order.
     * </p>
     *
     * @param context evaluation context
     * @return evaluated {@link ActionDefinition}
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Map<String, Object> arguments = new LinkedHashMap<>();

        for (Node child : getChildren()) {
            if (child instanceof KeyValueNode keyValue) {
                Object evaluated = keyValue.evaluate(context);

                if (evaluated instanceof Map.Entry<?, ?> entry) {
                    arguments.put((String) entry.getKey(), entry.getValue());
                }
            }
        }

        return new ActionDefinition.Default(getName(), arguments);
    }

}