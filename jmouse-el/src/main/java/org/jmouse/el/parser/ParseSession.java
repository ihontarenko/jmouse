package org.jmouse.el.parser;

import org.jmouse.el.node.Node;

import java.util.Objects;

/** Confined invocation state shared by all rules of one parse, with exception-safe depth. */
public final class ParseSession {

    private final ParseRuleRegistry rules;
    private final ParsingBudget budget;
    private final Thread owner = Thread.currentThread();
    private int depth;
    private long steps;

    public ParseSession(ParseRuleRegistry rules, ParsingBudget budget) {
        this.rules = Objects.requireNonNull(rules, "rules").snapshot();
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public <I, N extends Node> N parse(ParseRole<I, N> role, I input) {
        step();
        if (depth >= budget.maximumDepth()) {
            throw new ParsingLimitException("Parsing depth exceeded in " + role.name());
        }
        depth++;
        try {
            N node = role.nodeType().cast(Objects.requireNonNull(
                    rules.rule(role).parse(role.inputType().cast(input), this), "parsed node"));
            if (node.getParent() != null) {
                throw new IllegalStateException("A parse rule must return a detached root: " + role.name());
            }
            return node;
        } finally {
            depth--;
        }
    }

    /** Counts input consumption or another bounded unit of grammar work. */
    public void step() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("A parse session is confined to its owning thread");
        }
        if (steps >= budget.maximumSteps()) {
            throw new ParsingLimitException("Parsing work budget exceeded");
        }
        steps++;
    }

    public long steps() {
        return steps;
    }

    public ParseRuleRegistry rules() {
        return rules;
    }
}
