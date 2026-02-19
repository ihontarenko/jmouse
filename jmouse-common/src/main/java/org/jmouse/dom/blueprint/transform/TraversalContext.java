package org.jmouse.dom.blueprint.transform;

import org.jmouse.dom.blueprint.Blueprint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

public final class TraversalContext {

    private final Deque<Blueprint.ElementBlueprint> ancestors = new ArrayDeque<>();

    private static final TraversalContext EMPTY = new TraversalContext();

    public static TraversalContext empty() {
        return EMPTY;
    }

    public void enter(Blueprint.ElementBlueprint element) {
        ancestors.push(element);
    }

    public void exit() {
        ancestors.pop();
    }

    /**
     * Current depth in traversal.
     *
     * <p>0 means "root level" (no ancestors yet), 1 means "inside 1 element", etc.</p>
     */
    public int getDepth() {
        return ancestors.size();
    }

    /**
     * Parent element of the currently visited node (nearest ancestor).
     *
     * <p>Name kept as-is because your ContextMatch already calls context.isParent().</p>
     */
    public Blueprint.ElementBlueprint isParent() {
        return ancestors.peek();
    }

    public List<Blueprint.ElementBlueprint> ancestors() {
        return List.copyOf(ancestors);
    }

    public Blueprint.ElementBlueprint ancestor() {
        return ancestors.peek();
    }

    public boolean hasAncestor(String tagName) {
        for (Blueprint.ElementBlueprint element : ancestors) {
            if (element.tagName().equalsIgnoreCase(tagName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAncestor(Predicate<Blueprint.ElementBlueprint> predicate) {
        for (Blueprint.ElementBlueprint ancestor : ancestors) {
            if (predicate.test(ancestor)) {
                return true;
            }
        }
        return false;
    }
}
