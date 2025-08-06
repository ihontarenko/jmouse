package org.jmouse.el.renderable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stack of renderable templates for managing view inheritance.
 * <p>
 * The {@code Inheritance} interface provides operations to manage a hierarchy of
 * {@link Template} instances, such as pushing a new view onto the stack (inherit),
 * ascending to a parent view, descending to a child view, and retrieving the current
 * child or parent view.
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
     * Pushes the specified view onto the inheritance stack.
     *
     * @param template the {@link Template} to inherit.
     */
    void inherit(Template template);

    /**
     * Ascends one level in the inheritance hierarchy.
     * <p>
     * Typically, this operation removes the current child view from the stack,
     * moving the pointer to the parent view.
     * </p>
     */
    void ascend();

    /**
     * Descends one level in the inheritance hierarchy.
     * <p>
     * This operation allows navigating into a child view if available.
     * </p>
     */
    void descend();

    /**
     * Returns the current child view from the inheritance stack.
     *
     * @return the current child {@link Template}, or {@code null} if the stack is empty.
     */
    Template getChild();

    /**
     * Returns the current view from the inheritance stack.
     *
     * @return the current {@link Template}
     */
    Template getCurrent();

    /**
     * Returns the current parent view from the inheritance stack.
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
     * Checks if the inheritance stack currently has a child view.
     *
     * @return {@code true} if a child view exists; {@code false} otherwise.
     */
    default boolean hasChild() {
        return getChild() != null;
    }

    /**
     * Checks if the inheritance stack currently has a parent view.
     *
     * @return {@code true} if a parent view exists; {@code false} otherwise.
     */
    default boolean hasParent() {
        return getParent() != null;
    }
}
