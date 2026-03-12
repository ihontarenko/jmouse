package org.jmouse.action.adapter.el;

import org.jmouse.action.ActionDefinition;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.KeyValueNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AST node representing an action definition expression. ⚙️
 *
 * <p>
 * Supported form:
 * </p>
 *
 * <pre>{@code
 * @[namespace:name]{'key':'value'}
 * }</pre>
 *
 * <p>
 * The node stores:
 * </p>
 * <ul>
 *     <li>action namespace</li>
 *     <li>action name</li>
 *     <li>argument child nodes, usually {@link KeyValueNode}</li>
 * </ul>
 *
 * <p>
 * During evaluation, child key-value nodes are resolved into an ordered
 * argument map and wrapped into an {@link ActionDefinition}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * @[default:autoload]{'source':'user','mode':'lazy'}
 * }</pre>
 */
public class ActionDefinitionNode extends AbstractExpression {

    /**
     * Action name.
     */
    private String name;

    /**
     * Action namespace.
     */
    private String namespace;

    /**
     * Optional preconfigured arguments.
     */
    private Map<String, Object> arguments;

    /**
     * Returns action name.
     *
     * @return action name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets action name.
     *
     * @param name action name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns action namespace.
     *
     * @return action namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets action namespace.
     *
     * @param namespace action namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
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
     * Each child {@link KeyValueNode} is evaluated into a map entry.
     * Resolved entries are collected into a {@link LinkedHashMap}
     * to preserve declaration order.
     * </p>
     *
     * @param context evaluation context
     *
     * @return evaluated action definition
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

        String name = getNamespace() + ":" + getName();

        return new ActionDefinition.Default(name, arguments);
    }

}