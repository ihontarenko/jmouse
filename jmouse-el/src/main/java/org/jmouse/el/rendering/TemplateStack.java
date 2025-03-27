package org.jmouse.el.rendering;

/**
 * 🔄 Represents a stack of renderable entities for template inheritance.
 * <p>
 * The {@code EntityStack} maintains a hierarchy of {@link Template} instances,
 * enabling the management of parent-child relationships during template rendering.
 * It supports operations to inherit a new template, ascend or descend in the hierarchy,
 * and retrieve the current child or parent entity.
 * </p>
 */
public sealed interface TemplateStack permits Inheritance {

    /**
     * Returns an empty {@code EntityStack} instance.
     *
     * @return an empty EntityStack
     */
    static TemplateStack empty() {
        return new Inheritance();
    }

    /**
     * Inherits a new template by pushing it onto the hierarchy stack.
     *
     * @param template the renderable entity to inherit
     */
    void inherit(Template template);

    /**
     * Ascends the hierarchy, moving up one level.
     */
    void ascend();

    /**
     * Descends the hierarchy, moving down one level.
     */
    void descend();

    /**
     * Returns the current child entity from the stack.
     *
     * @return the current child {@link Template}, or {@code null} if none exists
     */
    Template getChild();

    /**
     * Returns the current parent entity from the stack.
     *
     * @return the current parent {@link Template}, or {@code null} if none exists
     */
    Template getParent();

    /**
     * Checks if a child entity exists in the stack.
     *
     * @return {@code true} if a child entity exists; {@code false} otherwise
     */
    default boolean hasChild() {
        return getChild() != null;
    }

    /**
     * Checks if a parent entity exists in the stack.
     *
     * @return {@code true} if a parent entity exists; {@code false} otherwise
     */
    default boolean hasParent() {
        return getParent() != null;
    }
}
