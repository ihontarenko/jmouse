package org.jmouse.el.node;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A role-owned tree with one structural storage. Typed accessors and generic traversal see the
 * same children. Construction is confined to its owning thread; a frozen tree is read-only.
 * Legacy mutable nodes remain available through {@link AbstractNode}.
 */
public abstract class StructuredNode implements Node {

    private final NodeSchema<?> schema;
    private final Map<ChildRole<?>, List<Node>> values = new LinkedHashMap<>();
    private final List<Node> children;
    private final List<Trivia> leadingTrivia = new ArrayList<>();
    private Node parent;
    private Trivia trailingTrivia;
    private NodeLifecycle lifecycle = NodeLifecycle.BUILDING;

    protected StructuredNode(NodeSchema<?> schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
        for (ChildRole<?> role : schema.roles()) {
            values.put(role, new ArrayList<>());
        }
        children = Collections.unmodifiableList(new AbstractList<>() {
            @Override
            public Node get(int index) {
                Objects.checkIndex(index, size());
                int remaining = index;
                for (List<Node> roleValues : values.values()) {
                    if (remaining < roleValues.size()) {
                        return roleValues.get(remaining);
                    }
                    remaining -= roleValues.size();
                }
                throw new IndexOutOfBoundsException(index);
            }

            @Override
            public int size() {
                int count = 0;
                for (List<Node> roleValues : values.values()) {
                    count += roleValues.size();
                }
                return count;
            }
        });
    }

    public final NodeSchema<?> schema() {
        return schema;
    }

    public final NodeLifecycle lifecycle() {
        return lifecycle;
    }

    public final <N extends Node> N child(ChildRole<N> role) {
        List<Node> children = values(role);
        if (role.cardinality() == ChildCardinality.LIST || children.isEmpty()) {
            throw new IllegalStateException("Expected a single child in " + role.name());
        }
        return role.type().cast(children.getFirst());
    }

    public final <N extends Node> Optional<N> optionalChild(ChildRole<N> role) {
        List<Node> children = values(role);
        if (role.cardinality() == ChildCardinality.LIST) {
            throw new IllegalArgumentException("A list role is not a single child: " + role.name());
        }
        return children.isEmpty() ? Optional.empty() : Optional.of(role.type().cast(children.getFirst()));
    }

    public final <N extends Node> List<N> children(ChildRole<N> role) {
        List<Node> children = values(role);
        return Collections.unmodifiableList(new AbstractList<>() {
            @Override
            public N get(int index) {
                return role.type().cast(children.get(index));
            }

            @Override
            public int size() {
                return children.size();
            }
        });
    }

    public final <N extends Node> void setChild(ChildRole<N> role, N child) {
        requireBuilding();
        List<Node> children = values(role);
        if (role.cardinality() == ChildCardinality.LIST) {
            throw new IllegalArgumentException("Use addChild for list role " + role.name());
        }
        if (!children.isEmpty() && children.getFirst() == child) {
            return;
        }
        if (child != null) {
            validateAttachment(role, child);
        }
        if (!children.isEmpty()) {
            detach(children.removeFirst());
        }
        if (child != null) {
            attach(child);
            children.add(child);
        }
    }

    public final <N extends Node> void addChild(ChildRole<N> role, N child) {
        requireBuilding();
        List<Node> children = values(role);
        if (role.cardinality() != ChildCardinality.LIST) {
            throw new IllegalArgumentException("Use setChild for single role " + role.name());
        }
        validateAttachment(role, Objects.requireNonNull(child, "child"));
        attach(child);
        children.add(child);
    }

    public final <N extends Node> N removeChild(ChildRole<N> role, int index) {
        requireBuilding();
        List<Node> children = values(role);
        Node removed = children.remove(index);
        detach(removed);
        return role.type().cast(removed);
    }

    /** Validates the complete tree before changing any lifecycle state. */
    public final void freeze() {
        if (lifecycle == NodeLifecycle.FROZEN) {
            return;
        }
        if (parent != null) {
            throw new IllegalStateException("Freeze the ownership root, not an attached subtree");
        }
        List<StructuredNode> validated = new ArrayList<>();
        Deque<StructuredNode> pending = new ArrayDeque<>();
        pending.push(this);
        while (!pending.isEmpty()) {
            StructuredNode node = pending.pop();
            if (!node.schema.kind().type().isInstance(node)) {
                throw new IllegalStateException("Node does not match schema " + node.schema.kind().name());
            }
            for (var entry : node.values.entrySet()) {
                if (entry.getKey().cardinality() == ChildCardinality.REQUIRED && entry.getValue().size() != 1) {
                    throw new IllegalStateException("Missing required role " + entry.getKey().name());
                }
            }
            node.validateStructure();
            validated.add(node);
            for (Node child : node.children) {
                if (!(child instanceof StructuredNode structured)) {
                    throw new IllegalStateException("A frozen tree cannot contain an unmanaged child");
                }
                pending.push(structured);
            }
        }
        for (StructuredNode node : validated) {
            node.lifecycle = NodeLifecycle.FROZEN;
        }
    }

    /** Language-specific structural invariants, without resolution or evaluation. */
    protected void validateStructure() {
    }

    protected final void requireBuilding() {
        if (lifecycle != NodeLifecycle.BUILDING) {
            throw new IllegalStateException("A frozen node cannot be changed");
        }
    }

    private List<Node> values(ChildRole<?> role) {
        List<Node> children = values.get(Objects.requireNonNull(role, "role"));
        if (children == null) {
            throw new IllegalArgumentException("Unknown role " + role.name() + " in " + schema.kind().name());
        }
        return children;
    }

    private void validateAttachment(ChildRole<?> role, Node child) {
        role.type().cast(child);
        if (child.getParent() != null) {
            throw new IllegalArgumentException("Child already belongs to a parent");
        }
        for (Node ancestor = this; ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor == child) {
                throw new IllegalArgumentException("Structural cycles are not allowed");
            }
        }
        if (child instanceof StructuredNode structured) {
            structured.requireBuilding();
        }
    }

    private void attach(Node child) {
        if (child instanceof StructuredNode structured) {
            structured.parent = this;
        } else {
            child.setParent(this);
        }
    }

    private void detach(Node child) {
        if (child instanceof StructuredNode structured) {
            structured.parent = null;
        } else {
            child.setParent(null);
        }
    }

    @Override
    public final Node getParent() {
        return parent;
    }

    @Override
    public final void setParent(Node parent) {
        throw new UnsupportedOperationException("Parent ownership is controlled by child roles");
    }

    @Override
    public final void add(Node node) {
        throw new UnsupportedOperationException("A structural child needs an explicit role");
    }

    @Override
    public final List<Node> getChildren() {
        return children;
    }

    @Override
    public final List<Trivia> getLeadingTrivia() {
        return Collections.unmodifiableList(leadingTrivia);
    }

    @Override
    public final void addLeadingTrivia(List<Trivia> trivia) {
        requireBuilding();
        leadingTrivia.addAll(List.copyOf(trivia));
    }

    @Override
    public final Trivia getTrailingTrivia() {
        return trailingTrivia;
    }

    @Override
    public final void setTrailingTrivia(Trivia trivia) {
        requireBuilding();
        trailingTrivia = trivia;
    }

    /** Dispatches one node; child traversal belongs to the engine. */
    protected void dispatch(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final void accept(Visitor visitor) {
        Objects.requireNonNull(visitor, "visitor");
        Deque<TraversalFrame> frames = new ArrayDeque<>();
        frames.push(new TraversalFrame(this));
        while (!frames.isEmpty()) {
            TraversalFrame frame = frames.peek();
            if (frame.children == null) {
                TraversalAction action = Objects.requireNonNull(visitor.enter(frame.node), "traversal action");
                if (action == TraversalAction.STOP) {
                    return;
                }
                frame.node.dispatch(visitor);
                frame.children = action == TraversalAction.SKIP_CHILDREN
                        ? Collections.emptyIterator() : frame.node.children.iterator();
            }
            if (frame.children.hasNext()) {
                Node child = frame.children.next();
                if (child instanceof StructuredNode structured) {
                    frames.push(new TraversalFrame(structured));
                } else {
                    child.accept(visitor);
                }
            } else {
                visitor.leave(frame.node);
                frames.pop();
            }
        }
    }

    private static final class TraversalFrame {
        private final StructuredNode node;
        private Iterator<Node> children;

        private TraversalFrame(StructuredNode node) {
            this.node = node;
        }
    }
}
