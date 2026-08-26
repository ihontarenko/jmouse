package org.jmouse.mapper.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A run of rules, the bindings they may use, and the fragments they pull in.
 *
 * <p>One type serves {@code always}, {@code from} and {@code fragment}, because the three differ in
 * where they sit and what they apply to, never in what may be written inside them. ⚠️ Three near-copies
 * would be three places to add the next construct, and the day one of them is missed is the day
 * {@code always} quietly stops accepting something {@code from} accepts.</p>
 *
 * <p>Rules are keyed by target property and keep insertion order. A property named twice is a mistake
 * worth refusing rather than a precedence question — {@link #add(RuleNode)} hands the first rule back so
 * that the parser can say so, which is where the refusal is written. The order is what a reader sees
 * when the block is written back out.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RuleBlockNode extends AbstractExpression {

    private final Map<String, RuleNode> rules    = new LinkedHashMap<>();
    private final List<LetNode>         bindings = new ArrayList<>();
    private final List<IncludeNode>     includes = new ArrayList<>();

    /**
     * Adds a rule.
     *
     * @param rule the rule to add
     * @return the rule already present for that property, or {@code null} when it is the first
     */
    public RuleNode add(RuleNode rule) {
        return rules.putIfAbsent(rule.getProperty(), rule);
    }

    /** @return every rule, in the order written */
    public Map<String, RuleNode> getRules() {
        return rules;
    }

    /**
     * Adds a named sub-expression.
     *
     * <p>⚠️ Order is the declaration order and it matters: a binding may reference an earlier one, so
     * a list rather than a map, and never sorted.</p>
     *
     * @param binding the binding to add
     */
    public void add(LetNode binding) {
        bindings.add(binding);
    }

    /** @return the bindings, in declaration order */
    public List<LetNode> getBindings() {
        return bindings;
    }

    /**
     * Pulls a fragment's rules into this block.
     *
     * @param include the line, carrying the fragment's name and where it was written
     */
    public void include(IncludeNode include) {
        includes.add(include);
    }

    /** @return the fragments this block includes, in the order written */
    public List<IncludeNode> getIncludes() {
        return includes;
    }

    /** @return {@code true} when nothing at all was written in this block */
    public boolean isEmpty() {
        return rules.isEmpty() && bindings.isEmpty() && includes.isEmpty();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "RuleBlock[%d rules, %d lets, %d includes]"
                .formatted(rules.size(), bindings.size(), includes.size());
    }
}
