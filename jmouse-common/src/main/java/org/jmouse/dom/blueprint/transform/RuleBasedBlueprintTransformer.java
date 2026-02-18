package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.*;
import org.jmouse.dom.blueprint.Blueprint.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transformer that applies ordered rules to every node in the blueprint tree.
 */
public final class RuleBasedBlueprintTransformer implements BlueprintTransformer {

    private static final Comparator<Rule> ORDERING = Comparator.comparingInt(Rule::order).reversed();

    private final List<Rule> rules;

    private RuleBasedBlueprintTransformer(List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public Blueprint transform(Blueprint blueprint, RenderingExecution execution) {
        Verify.nonNull(blueprint, "blueprint");
        Verify.nonNull(execution, "execution");
        return transformNode(blueprint, execution);
    }

    private Blueprint transformNode(Blueprint node, RenderingExecution execution) {
        Blueprint current = applyRules(node, execution);

        return switch (current) {
            case ElementBlueprint element -> {
                List<Blueprint> children = new ArrayList<>(element.children().size());
                for (Blueprint child : element.children()) {
                    children.add(transformNode(child, execution));
                }
                yield new ElementBlueprint(element.tagName(), element.attributes(), List.copyOf(children));
            }
            case ConditionalBlueprint conditional -> {
                List<Blueprint> whenTrue = mapList(conditional.whenTrue(), execution);
                List<Blueprint> whenFalse = mapList(conditional.whenFalse(), execution);
                yield new ConditionalBlueprint(conditional.predicate(), whenTrue, whenFalse);
            }
            case RepeatBlueprint repeat -> {
                List<Blueprint> body = mapList(repeat.body(), execution);
                yield new RepeatBlueprint(repeat.collection(), repeat.itemVariableName(), body);
            }
            case TextBlueprint text -> text;
            case IncludeBlueprint include -> include;
            case null -> throw new IllegalStateException("Unsupported blueprint node.");
        };
    }

    private List<Blueprint> mapList(List<Blueprint> nodes, RenderingExecution execution) {
        List<Blueprint> result = new ArrayList<>(nodes.size());
        for (Blueprint node : nodes) {
            result.add(transformNode(node, execution));
        }
        return List.copyOf(result);
    }

    private Blueprint applyRules(Blueprint node, RenderingExecution execution) {
        Blueprint current = node;
        for (Rule rule : rules) {
            if (rule.matcher().matches(current)) {
                current = rule.change().apply(current, execution);
            }
        }
        return current;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<Rule> rules = new ArrayList<>();

        public Builder rule(int order, BlueprintMatcher matcher, BlueprintChange change) {
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

    public record Rule(int order, BlueprintMatcher matcher, BlueprintChange change) {}
}
