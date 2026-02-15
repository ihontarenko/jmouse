package org.jmouse.validator.constraint.registry;

import org.jmouse.util.Strings;
import org.jmouse.validator.constraint.api.Constraint;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ConstraintTypeRegistry {

    private final Map<String, Class<? extends Constraint>> types = new LinkedHashMap<>();

    public ConstraintTypeRegistry register(String name, Class<? extends Constraint> type) {
        types.put(Strings.normalize(name, String::toLowerCase), type);
        return this;
    }

    public Optional<Class<? extends Constraint>> resolve(String name) {
        return Optional.ofNullable(types.get(Strings.normalize(name, String::toLowerCase)));
    }

}
