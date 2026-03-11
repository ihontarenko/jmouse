package org.jmouse.action;

import org.jmouse.core.Verify;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.core.Verify.notBlank;

/**
 * Default in-memory implementation of {@link ActionRegistry}. 🧱
 *
 * <p>
 * Stores {@link ActionDescriptor}s in a thread-safe {@link ConcurrentHashMap}
 * and provides basic registration and lookup operations.
 * </p>
 *
 * <p>
 * This registry is typically used by {@link ActionExecutor} to resolve
 * {@link ActionHandler}s during action execution.
 * </p>
 */
public class SimpleActionRegistry implements ActionRegistry {

    /**
     * Registered action descriptors indexed by action name.
     */
    private final Map<String, ActionDescriptor> descriptors = new ConcurrentHashMap<>();

    /**
     * Registers the given action descriptor.
     *
     * @param descriptor action descriptor
     * @throws ActionRegistrationException if an action with the same name
     *                                     is already registered
     */
    @Override
    public void register(ActionDescriptor descriptor) {
        Verify.nonNull(descriptor, "descriptor");

        String actionName = descriptor.name();
        ActionDescriptor previous = descriptors.putIfAbsent(actionName, descriptor);

        if (previous != null) {
            throw new ActionRegistrationException(
                    "Action '%s' is already registered.".formatted(actionName)
            );
        }
    }

    /**
     * Returns descriptor for the given action name.
     *
     * @param name action name
     * @return action descriptor
     * @throws ActionNotFoundException if no action is registered under the name
     */
    @Override
    public ActionDescriptor getDescriptor(String name) {
        notBlank(name, "name");

        ActionDescriptor descriptor = descriptors.get(name);

        if (descriptor == null) {
            throw new ActionNotFoundException(name);
        }

        return descriptor;
    }

    /**
     * Checks whether an action with the given name is registered.
     *
     * @param name action name
     * @return {@code true} if descriptor exists
     */
    @Override
    public boolean contains(String name) {
        return descriptors.containsKey(notBlank(name, "name"));
    }
}