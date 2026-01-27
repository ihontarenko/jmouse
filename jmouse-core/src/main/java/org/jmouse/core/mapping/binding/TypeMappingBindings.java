package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TypeMappingBindings {

    private final Class<?>                  sourceType;
    private final Class<?>                  targetType;
    private final Map<String, FieldBinding> bindings;

    public TypeMappingBindings(Class<?> sourceType, Class<?> targetType, Map<String, FieldBinding> bindings) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
        this.bindings = Collections.unmodifiableMap(new LinkedHashMap<>(bindings));
    }

    public Class<?> sourceType() {
        return sourceType;
    }

    public Class<?> targetType() {
        return targetType;
    }

    public FieldBinding find(String targetName) {
        return bindings.get(targetName);
    }

}
