package org.jmouse.el.renderable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stack of renderable templates for managing template inheritance.
 * <p>
 * The {@code Inheritance} interface provides operations to manage a hierarchy of
 * {@link Template} instances, such as pushing a new template onto the stack (inherit),
 * ascending to a parent template, descending to a child template, and retrieving the current
 * child or parent template.
 * </p>
 * <p>
 * This interface is sealed and its permitted implementation is {@link LinkedListInheritance}.
 * </p>
 */
public sealed interface Inheritance permits LinkedListInheritance {

    /**
     * Returns an empty {@code Inheritance} stack instance.
     *
     * @return an empty {@code Inheritance} stack.
     */
    static Inheritance empty() {
        return new LinkedListInheritance();
    }

    /**
     * Pushes the specified template onto the inheritance stack.
     *
     * @param template the {@link Template} to inherit.
     */
    void inherit(Template template);

    /**
     * Ascends one level in the inheritance hierarchy.
     * <p>
     * Typically, this operation removes the current child template from the stack,
     * moving the pointer to the parent template.
     * </p>
     */
    void ascend();

    /**
     * Descends one level in the inheritance hierarchy.
     * <p>
     * This operation allows navigating into a child template if available.
     * </p>
     */
    void descend();

    /**
     * Returns the current child template from the inheritance stack.
     *
     * @return the current child {@link Template}, or {@code null} if the stack is empty.
     */
    Template getChild();

    /**
     * Returns the current template from the inheritance stack.
     *
     * @return the current {@link Template}
     */
    Template getCurrent();

    /**
     * Returns the current parent template from the inheritance stack.
     *
     * @return the current parent {@link Template}, or {@code null} if no parent exists.
     */
    Template getParent();

    /**
     * Returns a list representation of the entire inheritance stack.
     *
     * @return a {@link List} of {@link Template} instances representing the stack.
     */
    List<Template> getStack();

    default Template getLower() {
        return getStack().getFirst();
    }

    default Template getUpper() {
        return getStack().getLast();
    }

    /**
     * Checks if the inheritance stack currently has a child template.
     *
     * @return {@code true} if a child template exists; {@code false} otherwise.
     */
    default boolean hasChild() {
        return getChild() != null;
    }

    /**
     * Checks if the inheritance stack currently has a parent template.
     *
     * @return {@code true} if a parent template exists; {@code false} otherwise.
     */
    default boolean hasParent() {
        return getParent() != null;
    }
}
