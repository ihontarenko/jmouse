package org.jmouse.el.renderable;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a stack for managing view inheritance hierarchy.
 * <p>
 * This implementation uses a {@link LinkedList} to store {@link Template} instances
 * representing the inheritance chain. The {@code depth} field indicates the current
 * position in the hierarchy: <br>
 * - {@code getParent()} returns the view at the current depth. <br>
 * - {@code getChild()} returns the view at depth - 1 (i.e. the more specific, child view).
 * </p>
 */
public final class LinkedListInheritance implements Inheritance {

    private final LinkedList<Template> stack = new LinkedList<>();
    private       int                  depth = 0;

    /**
     * Adds a new view to the inheritance stack.
     *
     * @param template the view to add
     */
    @Override
    public void inherit(Template template) {
        stack.addLast(template);
    }

    /**
     * Moves one level deeper in the hierarchy.
     * <p>
     * Increments the depth if a child view exists.
     * </p>
     *
     * @throws IllegalStateException if attempting to ascend beyond the available hierarchy
     */
    @Override
    public void ascend() {
        if (depth < stack.size()) {
            depth++;
        }
    }

    /**
     * Moves one level up in the hierarchy.
     * <p>
     * Decrements the depth if it is greater than zero.
     * </p>
     */
    @Override
    public void descend() {
        if (depth > 0) {
            depth--;
        }
    }

    /**
     * Returns the current child view from the stack.
     * <p>
     * This is the view at level {@code depth - 1}. If {@code depth} is 0, no child is available.
     * </p>
     *
     * @return the child {@link Template}, or {@code null} if not available
     */
    @Override
    public Template getChild() {
        return (depth > 0) ? stack.get(depth - 1) : null;
    }

    /**
     * Returns the current view from the inheritance stack.
     *
     * @return the current {@link Template}
     */
    @Override
    public Template getCurrent() {
        return stack.get(depth);
    }

    /**
     * Returns the current parent view from the stack.
     * <p>
     * This is the view at the current {@code depth} in the stack.
     * </p>
     *
     * @return the parent {@link Template}, or {@code null} if not available
     */
    @Override
    public Template getParent() {
        return (stack.size() - 1 > depth) ? stack.get(depth + 1) : null;
    }

    /**
     * Returns a list representation of the entire inheritance stack.
     *
     * @return a {@link List} of {@link Template} instances representing the stack.
     */
    @Override
    public List<Template> getStack() {
        return List.copyOf(stack);
    }
}
