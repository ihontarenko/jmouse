package org.jmouse.dom.blueprint.transform;

import org.jmouse.dom.blueprint.Blueprint.ElementBlueprint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Traversal context used during blueprint transformation.
 *
 * <p>Provides access to the current node, its ancestors, and traversal depth.</p>
 */
public final class TraversalContext {

    private final Deque<ElementBlueprint> ancestors = new ArrayDeque<>();

    void pushAncestor(ElementBlueprint ancestor) {
        ancestors.push(Objects.requireNonNull(ancestor, "ancestor"));
    }

    void popAncestor() {
        ancestors.pop();
    }

    public int depth() {
        return ancestors.size();
    }

    public List<ElementBlueprint> ancestors() {
        return List.copyOf(ancestors);
    }

    public ElementBlueprint parentOrNull() {
        return ancestors.peek();
    }

    public boolean hasAncestorTagName(String tagName) {
        for (ElementBlueprint ancestor : ancestors) {
            if (ancestor.tagName().equalsIgnoreCase(tagName)) {
                return true;
            }
        }
        return false;
    }
}
