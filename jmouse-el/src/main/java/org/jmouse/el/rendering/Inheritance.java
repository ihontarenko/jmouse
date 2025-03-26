package org.jmouse.el.rendering;

import java.util.LinkedList;

/**
 * 🔄 Represents a stack for managing template inheritance hierarchy.
 * <p>
 * This implementation of {@link EntityStack} uses a {@link LinkedList} to maintain
 * the hierarchy of {@link RenderableEntity} instances. The stack allows the renderer
 * to navigate between parent and child templates.
 * </p>
 */
public final class Inheritance implements EntityStack {

    private final LinkedList<RenderableEntity> stack = new LinkedList<>();
    private       int                          depth = 0;

    /**
     * Adds a new renderable entity to the hierarchy stack.
     *
     * @param entity the renderable entity to add
     */
    @Override
    public void inherit(RenderableEntity entity) {
        stack.addLast(entity);
    }

    /**
     * Moves one level deeper in the hierarchy.
     * <p>
     * Increments the depth if possible.
     * </p>
     */
    @Override
    public void ascend() {
        if (stack.size() - 1 > depth) {
            depth++;
        }
    }

    /**
     * Moves one level up in the hierarchy.
     * <p>
     * Decrements the depth if greater than zero.
     * </p>
     */
    @Override
    public void descend() {
        if (depth > 0) {
            depth--;
        }
    }

    /**
     * Returns the current child entity from the stack.
     *
     * @return the child {@link RenderableEntity}, or {@code null} if not available
     */
    @Override
    public RenderableEntity getChild() {
        return (depth > 0) ? stack.get(depth - 1) : null;
    }

    /**
     * Returns the current parent entity from the stack.
     *
     * @return the parent {@link RenderableEntity}, or {@code null} if not available
     */
    @Override
    public RenderableEntity getParent() {
        return (stack.size() - 1 > depth) ? stack.get(depth + 1) : null;
    }
}
