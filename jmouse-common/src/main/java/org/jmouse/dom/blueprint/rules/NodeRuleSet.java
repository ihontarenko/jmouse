package org.jmouse.dom.blueprint.rules;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.blueprint.RenderingExecution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies registered {@link NodeRule} instances to a node tree.
 */
public final class NodeRuleSet {

    private final List<NodeRule> rules = new ArrayList<>();

    public NodeRuleSet add(NodeRule rule) {
        rules.add(Verify.nonNull(rule, "rule"));
        return this;
    }

    public void applyAll(Node root, RenderingExecution execution) {
        Verify.nonNull(root, "root");
        Verify.nonNull(execution, "execution");

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
