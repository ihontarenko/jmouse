package org.jmouse.dom.meterializer.hooks;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.dom.meterializer.NodeRuleSet;
import org.jmouse.dom.meterializer.NodeRule;
import org.jmouse.meterializer.hooks.RenderingHook;

/**
 * Applies {@link NodeRule} rules to the materialized DOM tree. 🧩
 *
 * <p>
 * This hook runs <b>after</b> {@code TemplateMaterializer} has produced the final {@link Node}
 * tree and delegates rule execution to the configured {@link NodeRuleSet}.
 * </p>
 *
 * <p>
 * Intended use: post-processing / normalization steps that should operate on a fully
 * materialized DOM (e.g., adding attributes, wrapping nodes, pruning, restructuring).
 * </p>
 *
 * <h3>Ordering</h3>
 * <p>
 * Default order is very late ({@code 10_000}) to ensure it runs after most other hooks
 * (submission decoration, option selection, etc.). Adjust if your rule set must run earlier.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * NodeRuleSet rules = new NodeRuleSet()
 *     .add(new AddClassRule(TagName.INPUT, "form-control"))
 *     .add(new WrapRule(TagName.SELECT, TagName.DIV, Map.of("class", "select-wrapper")));
 *
 * RenderingHook<Node> hook = new NodeRuleHook(rules);
 * }</pre>
 *
 * <p>
 * ⚠ This hook may mutate the DOM tree depending on the rules applied.
 * </p>
 */
public final class NodeRuleHook implements RenderingHook<Node> {

    private final NodeRuleSet ruleSet;

    /**
     * Creates a hook that applies rules from the given {@link NodeRuleSet}.
     *
     * @param ruleSet rule set to apply (must not be {@code null})
     */
    public NodeRuleHook(NodeRuleSet ruleSet) {
        this.ruleSet = Verify.nonNull(ruleSet, "ruleSet");
    }

    /**
     * Runs very late in the hook chain.
     *
     * @return hook order
     */
    @Override
    public int order() {
        return 10_000;
    }

    /**
     * Applies all registered rules to the materialized root node.
     *
     * @param root materialized DOM root
     * @param execution rendering execution context
     */
    @Override
    public void afterMaterialize(Node root, RenderingExecution execution) {
        ruleSet.applyAll(root, execution);
    }
}