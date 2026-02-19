package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.*;

import java.util.*;

public final class RuleBasedBlueprintTransformer implements BlueprintTransformer {

    private static final Comparator<Rule> ORDERING =
            Comparator.comparingInt(Rule::order).reversed();

    private final List<Rule>         rules;
    private final BlueprintTraversal traversal = new BlueprintTraversal();

    private RuleBasedBlueprintTransformer(List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public Blueprint transform(Blueprint blueprint, RenderingExecution execution) {
        Verify.nonNull(blueprint, "blueprint");
        Verify.nonNull(execution, "execution");
        return traversal.traverse(blueprint, execution, this::rewriteNode);
    }

    private Blueprint rewriteNode(Blueprint node, RenderingExecution execution, TraversalContext context) {
        Blueprint current = node;

        for (Rule rule : rules) {
            if (rule.matcher().matches(current, context)) {
                current = rule.change().apply(current, execution);
            }
        }

        return current;
    }

    public record Rule(int order, ContextAwareMatcher matcher, BlueprintChange change) {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<Rule> rules = new ArrayList<>();

        public Builder rule(int order, ContextAwareMatcher matcher, BlueprintChange change) {
            Verify.nonNull(matcher, "matcher");
            Verify.nonNull(change, "change");
            rules.add(new Rule(order, matcher, change));
            rules.sort(ORDERING);
            return this;
        }

        public RuleBasedBlueprintTransformer build() {
            return new RuleBasedBlueprintTransformer(List.copyOf(rules));
        }
    }

}

