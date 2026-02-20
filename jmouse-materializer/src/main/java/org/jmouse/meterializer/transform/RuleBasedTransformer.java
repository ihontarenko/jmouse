package org.jmouse.meterializer.transform;

import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.meterializer.TemplateTransformer;

import java.util.*;

import static org.jmouse.core.Verify.nonNull;

public final class RuleBasedTransformer implements TemplateTransformer {

    private static final Comparator<Rule> ORDERING =
            Comparator.comparingInt(Rule::order).reversed();

    private final List<Rule>        rules;
    private final TemplateTraversal traversal = new TemplateTraversal();

    private RuleBasedTransformer(List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public NodeTemplate transform(NodeTemplate blueprint, RenderingExecution execution) {
        nonNull(blueprint, "blueprint");
        nonNull(execution, "execution");
        return traversal.traverse(blueprint, execution, this::rewriteNode);
    }

    private NodeTemplate rewriteNode(NodeTemplate node, RenderingExecution execution, TraversalContext context) {
        NodeTemplate current = node;

        for (Rule rule : rules) {
            if (rule.matcher().matches(current, context)) {
                current = rule.change().apply(current, execution);
            }
        }

        return current;
    }

    public record Rule(int order, ContextAwareMatcher matcher, TemplateChange change) {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<Rule> rules = new ArrayList<>();

        public Builder rule(int order, ContextAwareMatcher matcher, TemplateChange changer) {
            nonNull(matcher, "matcher");
            nonNull(changer, "changer");
            rules.add(new Rule(order, matcher, changer));
            rules.sort(ORDERING);
            return this;
        }

        public RuleBasedTransformer build() {
            return new RuleBasedTransformer(List.copyOf(rules));
        }
    }

}

