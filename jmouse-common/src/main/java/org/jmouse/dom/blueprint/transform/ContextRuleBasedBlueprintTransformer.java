package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.*;
import org.jmouse.dom.blueprint.Blueprint.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transformer that applies ordered context-aware rules to every node in the blueprint tree.
 */
public final class ContextRuleBasedBlueprintTransformer implements BlueprintTransformer {

    private static final Comparator<RuleEntry> ORDERING = Comparator.comparingInt(RuleEntry::order).reversed();

    private final List<RuleEntry> rules;

    private ContextRuleBasedBlueprintTransformer(List<RuleEntry> rules) {
        this.rules = rules;
    }

    @Override
    public Blueprint transform(Blueprint blueprint, RenderingExecution execution) {
        Verify.nonNull(blueprint, "blueprint");
        Verify.nonNull(execution, "execution");
        return transformNode(blueprint, execution, new TraversalContext());
    }

    private Blueprint transformNode(Blueprint node, RenderingExecution execution, TraversalContext context) {
        Blueprint current = applyRules(node, execution, context);

        return switch (current) {
            case ElementBlueprint element -> {
                context.pushAncestor(element);
                List<Blueprint> children = new ArrayList<>(element.children().size());
                for (Blueprint child : element.children()) {
                    children.add(transformNode(child, execution, context));
                }
                context.popAncestor();
                yield new ElementBlueprint(element.tagName(), element.attributes(), List.copyOf(children));
            }
            case ConditionalBlueprint conditional -> {
                List<Blueprint> whenTrue = mapList(conditional.whenTrue(), execution, context);
                List<Blueprint> whenFalse = mapList(conditional.whenFalse(), execution, context);
                yield new ConditionalBlueprint(conditional.predicate(), whenTrue, whenFalse);
            }
            case RepeatBlueprint repeat -> {
                List<Blueprint> body = mapList(repeat.body(), execution, context);
                yield new RepeatBlueprint(repeat.collection(), repeat.itemVariableName(), body);
            }
            case TextBlueprint text -> text;
            case IncludeBlueprint include -> include;
            case null -> throw new IllegalStateException("Unsupported blueprint node.");
        };
    }

    private List<Blueprint> mapList(List<Blueprint> nodes, RenderingExecution execution, TraversalContext context) {
        List<Blueprint> result = new ArrayList<>(nodes.size());
        for (Blueprint node : nodes) {
            result.add(transformNode(node, execution, context));
        }
        return List.copyOf(result);
    }

    private Blueprint applyRules(Blueprint node, RenderingExecution execution, TraversalContext context) {
        Blueprint current = node;

        for (RuleEntry ruleEntry : rules) {
            if (ruleEntry.matcher().matches(current, context)) {
                current = ruleEntry.change().apply(current, execution);
            }
        }

        return current;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<RuleEntry> rules = new ArrayList<>();

        public Builder rule(int order, ContextAwareMatcher matcher, BlueprintChange change) {
            Verify.nonNull(matcher, "matcher");
            Verify.nonNull(change, "change");
            rules.add(new RuleEntry(order, matcher, change));
            rules.sort(ORDERING);
            return this;
        }

        public Builder rule(int order, BlueprintMatcher matcher, BlueprintChange change) {
            return rule(order, ContextAwareMatcher.from(matcher), change);
        }

        public ContextRuleBasedBlueprintTransformer build() {
            return new ContextRuleBasedBlueprintTransformer(List.copyOf(rules));
        }
    }

    public record RuleEntry(int order, ContextAwareMatcher matcher, BlueprintChange change) {}
}
