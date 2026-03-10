package org.jmouse.action;

/**
 * Registry for action handlers. 🗂️
 */
public interface ActionRegistry {

    /**
     * Registers the given action descriptor.
     *
     * @param descriptor action descriptor
     */
    void register(ActionDescriptor descriptor);

    /**
     * Registers handler for the given action name.
     *
     * @param name action name
     * @param handler action handler
     */
    default void register(String name, ActionHandler handler) {
        register(new ActionDescriptor.Default(name, handler));
    }

    /**
     * Returns registered action descriptor.
     *
     * @param name action name
     * @return registered descriptor
     * @throws ActionNotFoundException if action is not registered
     */
    ActionDescriptor getDescriptor(String name);

    /**
     * Returns registered handler.
     *
     * @param name action name
     * @return action handler
     * @throws ActionNotFoundException if action is not registered
     */
    default ActionHandler get(String name) {
        return getDescriptor(name).handler();
    }

    /**
     * Returns whether the registry contains the given action.
     *
     * @param name action name
     * @return {@code true} if registered
     */
    boolean contains(String name);
}