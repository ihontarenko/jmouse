package org.jmouse.el.translate;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.StructuredNode;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Exact node dispatch, independent from traversal, source language and target language. */
public final class NodeOperationRegistry<C, R> {
    public record BindingDescription(String operation, String nodeKind, String implementation) {}

    private record Binding<C, R>(
            NodeKind<?> kind, String identity, BiFunction<StructuredNode, C, R> operation) {}

    private enum State {
        BUILDING,
        FROZEN
    }

    private final String operation;
    private final Map<String, Binding<C, R>> bindings;
    private final State state;

    public NodeOperationRegistry(String operation) {
        this(operation, new LinkedHashMap<>(), State.BUILDING);
    }

    private NodeOperationRegistry(
            String operation, Map<String, Binding<C, R>> bindings, State state) {
        if (Objects.requireNonNull(operation, "operation").isBlank()) {
            throw new IllegalArgumentException("An operation requires an identity");
        }
        this.operation = operation;
        this.bindings = bindings;
        this.state = state;
    }

    public synchronized <N extends Node> void bind(
            NodeKind<N> kind, String identity, NodeOperation<N, C, R> handler) {
        requireBuilding();
        Binding<C, R> binding = binding(kind, identity, handler);
        if (bindings.putIfAbsent(kind.name(), binding) != null) {
            throw new IllegalArgumentException("Duplicate node handler: " + kind.name());
        }
    }

    public synchronized <N extends Node> void replace(
            NodeKind<N> kind,
            String expectedIdentity,
            String identity,
            NodeOperation<N, C, R> handler) {
        requireBuilding();
        requireIdentity(kind, expectedIdentity);
        bindings.put(kind.name(), binding(kind, identity, handler));
    }

    public synchronized <N extends Node> void decorate(
            NodeKind<N> kind,
            String expectedIdentity,
            String identity,
            Function<NodeOperation<N, C, R>, NodeOperation<N, C, R>> decoration) {
        requireBuilding();
        Binding<C, R> previous = requireIdentity(kind, expectedIdentity);
        NodeOperation<N, C, R> adapter =
                (node, context) -> previous.operation().apply((StructuredNode) node, context);
        bindings.put(
                kind.name(),
                binding(
                        kind,
                        identity,
                        Objects.requireNonNull(decoration, "decoration").apply(adapter)));
    }

    public R apply(StructuredNode node, C context) {
        Binding<C, R> binding;
        synchronized (this) {
            binding = require(node.schema().kind());
        }
        return Objects.requireNonNull(
                binding.operation().apply(node, context), "Node operation result");
    }

    public synchronized boolean contains(NodeKind<?> kind) {
        Binding<C, R> binding = bindings.get(kind.name());
        return binding != null && binding.kind().equals(kind);
    }

    public synchronized BindingDescription explain(NodeKind<?> kind) {
        Binding<C, R> binding = require(kind);
        return new BindingDescription(operation, kind.name(), binding.identity());
    }

    public synchronized List<BindingDescription> descriptions() {
        return bindings.values().stream()
                .map(
                        binding ->
                                new BindingDescription(
                                        operation, binding.kind().name(), binding.identity()))
                .sorted(Comparator.comparing(BindingDescription::nodeKind))
                .toList();
    }

    public synchronized NodeOperationRegistry<C, R> snapshot() {
        return state == State.FROZEN
                ? this
                : new NodeOperationRegistry<>(operation, Map.copyOf(bindings), State.FROZEN);
    }

    public synchronized NodeOperationRegistry<C, R> fork() {
        return new NodeOperationRegistry<>(
                operation, new LinkedHashMap<>(bindings), State.BUILDING);
    }

    private Binding<C, R> requireIdentity(NodeKind<?> kind, String identity) {
        Binding<C, R> binding = require(kind);
        if (!binding.identity().equals(identity)) {
            throw new IllegalArgumentException(
                    "Node binding changed for "
                            + kind.name()
                            + ": expected "
                            + identity
                            + ", found "
                            + binding.identity());
        }
        return binding;
    }

    private Binding<C, R> require(NodeKind<?> kind) {
        Binding<C, R> binding = bindings.get(Objects.requireNonNull(kind, "kind").name());
        if (binding == null) {
            throw new NodeOperationBindingException(operation, kind.name());
        }
        if (!binding.kind().equals(kind)) {
            throw new IllegalArgumentException("Incompatible node kind contract: " + kind.name());
        }
        return binding;
    }

    private <N extends Node> Binding<C, R> binding(
            NodeKind<N> kind, String identity, NodeOperation<N, C, R> handler) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(handler, "handler");
        if (Objects.requireNonNull(identity, "identity").isBlank()) {
            throw new IllegalArgumentException("A node handler requires an identity");
        }
        return new Binding<>(
                kind, identity, (node, context) -> handler.apply(kind.type().cast(node), context));
    }

    private void requireBuilding() {
        if (state != State.BUILDING) {
            throw new IllegalStateException("A node operation snapshot cannot be changed");
        }
    }
}
