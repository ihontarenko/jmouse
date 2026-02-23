package org.jmouse.dom.meterializer;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.RenderingExecution;

/**
 * Contract for DOM post-processing rules applied after materialization. 🔧
 *
 * <p>
 * A {@code NodeRule} inspects and optionally mutates a {@link Node}
 * within the rendered DOM tree.
 * Rules are typically executed by {@link NodeRuleSet}.
 * </p>
 *
 * <h3>Execution lifecycle</h3>
 * <ul>
 *     <li>{@link #order()} defines rule priority (lower executes first).</li>
 *     <li>{@link #matches(Node, RenderingExecution)} determines applicability.</li>
 *     <li>{@link #apply(Node, RenderingExecution)} performs mutation.</li>
 * </ul>
 *
 * <h3>Typical use cases</h3>
 * <ul>
 *     <li>Auto-adding CSS classes</li>
 *     <li>Wrapping specific elements</li>
 *     <li>Injecting attributes</li>
 *     <li>Structural normalization</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public final class AddClassRule implements NodeRule {
 *
 *     @Override
 *     public int order() {
 *         return 100;
 *     }
 *
 *     @Override
 *     public boolean matches(Node node, RenderingExecution execution) {
 *         return node.getTagName() == TagName.INPUT;
 *     }
 *
 *     @Override
 *     public void apply(Node node, RenderingExecution execution) {
 *         node.addClass("form-control");
 *     }
 * }
 * }</pre>
 *
 * ⚠ Implementations are allowed to mutate the DOM tree.
 * Rule ordering should be designed carefully to avoid conflicts.
 */
public interface NodeRule {

    /**
     * Defines execution priority.
     *
     * <p>Lower values execute earlier.</p>
     *
     * @return rule order
     */
    int order();

    /**
     * Determines whether this rule should be applied to the given node.
     *
     * @param node       DOM node
     * @param execution  rendering execution context
     * @return {@code true} if the rule applies
     */
    boolean matches(Node node, RenderingExecution execution);

    /**
     * Applies rule logic to the node.
     *
     * @param node       DOM node to mutate
     * @param execution  rendering execution context
     */
    void apply(Node node, RenderingExecution execution);

}