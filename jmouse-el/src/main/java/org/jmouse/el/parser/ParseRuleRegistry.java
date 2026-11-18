package org.jmouse.el.parser;

import org.jmouse.el.node.Node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Typed role bindings with explicit replacement and immutable session snapshots. */
public final class ParseRuleRegistry {

    public record BindingDescription(String role, String implementation) {}

    private record Binding(ParseRole<?, ?> role, String identity, ParseRule<?, ?> rule) {}

    private final Map<String, Binding> bindings;
    private final State state;

    private enum State { BUILDING, FROZEN }

    public ParseRuleRegistry() {
        this(new LinkedHashMap<>(), State.BUILDING);
    }

    private ParseRuleRegistry(Map<String, Binding> bindings, State state) {
        this.bindings = bindings;
        this.state = state;
    }

    public synchronized <I, N extends Node> void bind(
            ParseRole<I, N> role, String identity, ParseRule<I, N> rule) {
        requireBuilding();
        Binding binding = binding(role, identity, rule);
        if (bindings.putIfAbsent(role.name(), binding) != null) {
            throw new IllegalArgumentException("Duplicate parse role: " + role.name());
        }
    }

    public synchronized <I, N extends Node> void replace(
            ParseRole<I, N> role, String expectedIdentity, String identity, ParseRule<I, N> rule) {
        requireBuilding();
        Binding replacement = binding(role, identity, rule);
        Binding previous = require(role);
        if (!previous.identity().equals(expectedIdentity)) {
            throw new IllegalArgumentException("Parse binding changed for " + role.name()
                    + ": expected " + expectedIdentity + ", found " + previous.identity());
        }
        bindings.put(role.name(), replacement);
    }

    public synchronized ParseRuleRegistry snapshot() {
        return state == State.FROZEN ? this
                : new ParseRuleRegistry(Map.copyOf(bindings), State.FROZEN);
    }

    public synchronized List<BindingDescription> descriptions() {
        return bindings.values().stream()
                .map(binding -> new BindingDescription(binding.role().name(), binding.identity()))
                .sorted(java.util.Comparator.comparing(BindingDescription::role)).toList();
    }

    public synchronized boolean contains(ParseRole<?, ?> role) {
        Binding binding = bindings.get(role.name());
        return binding != null && binding.role().equals(role);
    }

    @SuppressWarnings("unchecked")
    synchronized <I, N extends Node> ParseRule<I, N> rule(ParseRole<I, N> role) {
        // Checked at binding and lookup by both runtime input and result types.
        return (ParseRule<I, N>) require(role).rule();
    }

    private Binding require(ParseRole<?, ?> role) {
        Binding binding = bindings.get(Objects.requireNonNull(role, "role").name());
        if (binding == null) {
            throw new IllegalArgumentException("Unbound parse role: " + role.name());
        }
        if (!binding.role().equals(role)) {
            throw new IllegalArgumentException("Incompatible contract for parse role " + role.name());
        }
        return binding;
    }

    private static Binding binding(ParseRole<?, ?> role, String identity, ParseRule<?, ?> rule) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(rule, "rule");
        if (identity.isBlank()) {
            throw new IllegalArgumentException("A parse implementation needs an identity");
        }
        return new Binding(role, identity, rule);
    }

    private void requireBuilding() {
        if (state != State.BUILDING) {
            throw new IllegalStateException("A parser configuration snapshot cannot be changed");
        }
    }
}
