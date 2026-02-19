package org.jmouse.dom.template.hooks;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.template.RenderingExecution;
import org.jmouse.dom.template.rules.NodeRuleSet;

/**
 * Applies {@link org.jmouse.dom.template.rules.NodeRule} rules after materialization.
 */
public final class NodeRuleHook implements RenderingHook {

    private final NodeRuleSet ruleSet;

    public NodeRuleHook(NodeRuleSet ruleSet) {
        this.ruleSet = Verify.nonNull(ruleSet, "ruleSet");
    }

    @Override
    public int order() {
        return 10_000;
    }

    @Override
    public void afterMaterialize(Node root, RenderingExecution execution) {
        ruleSet.applyAll(root, execution);
    }
}
