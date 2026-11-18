package org.jmouse.el.grammar.definition;

import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.el.parser.ParsingBudget;

import java.util.List;
import java.util.Objects;

/** A validated entry role and its complete immutable bindings, for text or binary input. */
public final class GrammarDefinition<I, N extends Node> {

    private final ParseRole<I, N> entry;
    private final ParseRuleRegistry rules;

    public GrammarDefinition(ParseRole<I, N> entry, ParseRuleRegistry rules, List<ParseRole<?, ?>> required) {
        this.entry = Objects.requireNonNull(entry, "entry");
        this.rules = Objects.requireNonNull(rules, "rules").snapshot();
        if (!this.rules.contains(entry)) {
            throw new IllegalArgumentException("Unbound grammar entry " + entry.name());
        }
        for (ParseRole<?, ?> role : List.copyOf(required)) {
            if (!this.rules.contains(role)) {
                throw new IllegalArgumentException("Missing required grammar role " + role.name());
            }
        }
    }

    public N parse(I input, ParsingBudget budget) {
        return new ParseSession(rules, budget).parse(entry, input);
    }

    public ParseRuleRegistry bindings() {
        return rules;
    }
}
