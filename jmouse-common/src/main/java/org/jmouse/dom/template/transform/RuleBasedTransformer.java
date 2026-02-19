package org.jmouse.dom.template.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.template.*;

import java.util.*;

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
        Verify.nonNull(blueprint, "blueprint");
        Verify.nonNull(execution, "execution");
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

        public Builder rule(int order, ContextAwareMatcher matcher, TemplateChange change) {
            Verify.nonNull(matcher, "matcher");
            Verify.nonNull(change, "change");
            rules.add(new Rule(order, matcher, change));
            rules.sort(ORDERING);
            return this;
        }

        public RuleBasedTransformer build() {
            return new RuleBasedTransformer(List.copyOf(rules));
        }
    }

}

