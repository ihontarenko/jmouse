package org.jmouse.action;

import org.jmouse.core.Verify;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.core.Verify.notBlank;

/**
 * Default in-memory {@link ActionRegistry}. 🧱
 */
public class DefaultActionRegistry implements ActionRegistry {

    private final Map<String, ActionDescriptor> descriptors = new ConcurrentHashMap<>();

    @Override
    public void register(ActionDescriptor descriptor) {
        Verify.nonNull(descriptor, "descriptor");

        String           actionName = descriptor.name();
        ActionDescriptor previous   = descriptors.putIfAbsent(actionName, descriptor);

        if (previous != null) {
            throw new ActionRegistrationException(
                    "Action '%s' is already registered.".formatted(actionName)
            );
        }
    }

    @Override
    public ActionDescriptor getDescriptor(String name) {
        notBlank(name, "name");

        ActionDescriptor descriptor = descriptors.get(name);

        if (descriptor == null) {
            throw new ActionNotFoundException(name);
        }

        return descriptor;
    }

    @Override
    public boolean contains(String name) {
        return descriptors.containsKey(notBlank(name, "name"));
    }
}