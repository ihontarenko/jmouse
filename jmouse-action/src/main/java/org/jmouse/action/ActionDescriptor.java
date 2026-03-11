package org.jmouse.action;

import org.jmouse.core.Verify;

/**
 * Describes a registered action. 🧩
 *
 * <p>
 * An {@code ActionDescriptor} binds a unique action name to its
 * {@link ActionHandler}. It represents the registration unit used by
 * {@link ActionRegistry} to resolve and execute actions.
 * </p>
 *
 * <p>
 * This abstraction allows the action subsystem to evolve without
 * exposing internal registration details.
 * </p>
 */
public interface ActionDescriptor {

    /**
     * Returns the unique action name.
     *
     * @return action name
     */
    String name();

    /**
     * Returns the handler responsible for executing the action.
     *
     * @return action handler
     */
    ActionHandler handler();

    /**
     * Default immutable {@link ActionDescriptor} implementation. 🧱
     *
     * <p>
     * Performs basic validation of descriptor fields.
     * </p>
     */
    record Default(String name, ActionHandler handler) implements ActionDescriptor {

        /**
         * Creates descriptor and validates required fields.
         */
        public Default {
            Verify.notBlank(name, "name");
            Verify.nonNull(handler, "handler");
        }
    }

}