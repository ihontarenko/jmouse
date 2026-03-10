package org.jmouse.action;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.Verify.notBlank;

public class ActionTypeRegistry {

    private final Map<String, Class<?>> types = new LinkedHashMap<>();

    public ActionTypeRegistry register(String name, Class<?> type) {
        notBlank(name, "name");
        nonNull(type, "type");

        Class<?> previous = types.putIfAbsent(name, type);

        if (previous != null) {
            throw new ActionRegistrationException(
                    "Action type '%s' is already registered.".formatted(name)
            );
        }

        return this;
    }

    public Optional<Class<?>> resolve(String name) {
        return Optional.ofNullable(types.get(notBlank(name, "name")));
    }

    public boolean contains(String name) {
        return types.containsKey(notBlank(name, "name"));
    }
}