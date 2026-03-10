package org.jmouse.action;

import org.jmouse.core.Verify;

/**
 * Describes a registered action. 🧩
 *
 * <p>
 * A descriptor binds a unique action name to its handler.
 * This model provides a stable registration unit and allows
 * the action subsystem to evolve without changing the public API.
 * </p>
 */
public interface ActionDescriptor {

    /**
     * Returns the unique action name.
     */
    String name();

    /**
     * Returns the action handler.
     */
    ActionHandler handler();

    /**
     * Default immutable {@link ActionDescriptor} implementation. 🧱
     */
    record Default(String name, ActionHandler handler) implements ActionDescriptor {

        public Default {
            Verify.notBlank(name, "name");
            Verify.nonNull(handler, "handler");
        }
    }

}