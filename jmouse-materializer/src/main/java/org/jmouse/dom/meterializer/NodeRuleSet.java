package org.jmouse.dom.meterializer;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.RenderingExecution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.jmouse.core.Verify.nonNull;

/**
 * Container and executor for {@link NodeRule} definitions. 🧠
 *
 * <p>
 * {@code NodeRuleSet} stores a collection of rules and applies them
 * to a materialized DOM tree in a deterministic order.
 * </p>
 *
 * <h3>Execution model</h3>
 * <ul>
 *     <li>Rules are sorted by {@link NodeRule#order()}.</li>
 *     <li>The tree is traversed depth-first via {@link Node#execute(java.util.function.Consumer)}.</li>
 *     <li>For each node, all matching rules are executed.</li>
 * </ul>
 *
 * <p>
 * A rule is applied only if {@link NodeRule#matches(Node, RenderingExecution)} returns {@code true}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * NodeRuleSet rules = new NodeRuleSet()
 *     .add(new AddClassRule(TagName.INPUT, "form-control"))
 *     .add(new WrapRule(TagName.SELECT, TagName.DIV));
 *
 * rules.applyAll(rootNode, execution);
 * }</pre>
 *
 * <p>
 * ⚠ Rules may mutate the DOM tree (attributes, structure, wrapping, etc.).
 * Ensure rule ordering reflects intended transformation stages.
 * </p>
 */
public final class NodeRuleSet {

    private final List<NodeRule> rules = new ArrayList<>();

    /**
     * Adds a rule to this rule set.
     *
     * @param nodeRule rule to add (must not be {@code null})
     * @return this rule set (for fluent chaining)
     */
    public NodeRuleSet add(NodeRule nodeRule) {
        rules.add(nonNull(nodeRule, "nodeRule"));
        return this;
    }

    /**
     * Applies all registered rules to the given DOM tree.
     *
     * <p>
     * Rules are sorted by {@link NodeRule#order()} before execution.
     * </p>
     *
     * @param root root DOM node
     * @param execution rendering execution context
     */
    public void applyAll(Node root, RenderingExecution execution) {
        nonNull(root, "root");
        nonNull(execution, "execution");

        List<NodeRule> ordered = rules.stream()
                .sorted(Comparator.comparingInt(NodeRule::order))
                .toList();

        root.execute(node -> {
            for (NodeRule rule : ordered) {
                if (rule.matches(node, execution)) {
                    rule.apply(node, execution);
                }
            }
        });
    }
}