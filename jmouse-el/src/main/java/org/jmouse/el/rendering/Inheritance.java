package org.jmouse.el.rendering;

import java.util.LinkedList;

/**
 * 🔄 Represents a stack for managing template inheritance hierarchy.
 * <p>
 * This implementation of {@link TemplateStack} uses a {@link LinkedList} to maintain
 * the hierarchy of {@link Template} instances. The stack allows the renderer
 * to navigate between parent and child templates.
 * </p>
 */
public final class Inheritance implements TemplateStack {

    private final LinkedList<Template> stack = new LinkedList<>();
    private       int                  depth = 0;

    /**
     * Adds a new renderable entity to the hierarchy stack.
     *
     * @param template the renderable entity to add
     */
    @Override
    public void inherit(Template template) {
        stack.addLast(template);
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
     * @return the child {@link Template}, or {@code null} if not available
     */
    @Override
    public Template getChild() {
        return (depth > 0) ? stack.get(depth - 1) : null;
    }

    /**
     * Returns the current parent entity from the stack.
     *
     * @return the parent {@link Template}, or {@code null} if not available
     */
    @Override
    public Template getParent() {
        return (stack.size() - 1 > depth) ? stack.get(depth + 1) : null;
    }
}
