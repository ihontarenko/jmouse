package org.jmouse.dom.meterializer.rules;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.RenderingExecution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.jmouse.core.Verify.nonNull;

public final class NodeRuleSet {

    private final List<NodeRule> rules = new ArrayList<>();

    public NodeRuleSet add(NodeRule nodeRule) {
        rules.add(nonNull(nodeRule, "nodeRule"));
        return this;
    }

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
