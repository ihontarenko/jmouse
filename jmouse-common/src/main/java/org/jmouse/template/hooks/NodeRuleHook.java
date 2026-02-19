package org.jmouse.template.hooks;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.template.RenderingExecution;
import org.jmouse.template.rules.NodeRuleSet;
import org.jmouse.template.rules.NodeRule;

/**
 * Applies {@link NodeRule} rules after materialization.
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
