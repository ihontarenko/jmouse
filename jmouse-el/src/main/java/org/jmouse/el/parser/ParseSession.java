package org.jmouse.el.parser;

import org.jmouse.el.node.Node;
import org.jmouse.core.context.ContextKey;
import org.jmouse.core.context.execution.ExecutionContext;
import org.jmouse.core.context.execution.DefaultExecutionContext;

import java.util.Objects;
import java.util.function.Supplier;

/** Confined invocation state shared by all rules of one parse, with exception-safe depth. */
public final class ParseSession {

    private final ParseRuleRegistry rules;
    private final ParsingBudget budget;
    private final Thread owner = Thread.currentThread();
    private int depth;
    private long steps;
    private ExecutionContext context = new DefaultExecutionContext();

    public ParseSession(ParseRuleRegistry rules, ParsingBudget budget) {
        this.rules = Objects.requireNonNull(rules, "rules").snapshot();
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public <I, N extends Node> N parse(ParseRole<I, N> role, I input) {
        return scoped(role.name(), () -> {
            N node = role.nodeType().cast(Objects.requireNonNull(
                    rules.rule(role).parse(role.inputType().cast(input), this), "parsed node"));
            if (node.getParent() != null) {
                throw new IllegalStateException("A parse rule must return a detached root: " + role.name());
            }
            return node;
        });
    }

    /** Shares the session budget with recursive combinators that do not return a node per step. */
    public <T> T scoped(String operation, Supplier<T> invocation) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(invocation, "invocation");
        step();
        if (depth >= budget.maximumDepth()) {
            throw new ParsingLimitException(ParsingLimitKind.DEPTH, "Parsing depth exceeded in " + operation);
        }
        depth++;
        try {
            return invocation.get();
        } finally {
            depth--;
        }
    }

    /** Counts input consumption or another bounded unit of grammar work. */
    public void step() {
        requireOwner();
        if (steps >= budget.maximumSteps()) {
            throw new ParsingLimitException(ParsingLimitKind.WORK, "Parsing work budget exceeded");
        }
        steps++;
    }

    /** Reuses core's typed context while keeping parser state local to this session. */
    public <T> T context(ContextKey<T> key) {
        requireOwner();
        return key.type().cast(context.get(key));
    }

    public <T, R> R with(ContextKey<T> key, T value, Supplier<R> invocation) {
        Objects.requireNonNull(key, "key");
        return scoped(key.name(), () -> {
            ExecutionContext previous = context;
            context = context.with(key, key.type().cast(value));
            try {
                return invocation.get();
            } finally {
                context = previous;
            }
        });
    }

    private void requireOwner() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("A parse session is confined to its owning thread");
        }
    }

    public long steps() {
        return steps;
    }

    public ParseRuleRegistry rules() {
        return rules;
    }
}
