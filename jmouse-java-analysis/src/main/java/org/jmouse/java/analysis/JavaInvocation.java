package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.type.JavaExecutableType;
import org.jmouse.java.model.type.JavaType;

import java.util.List;
import java.util.Objects;

public record JavaInvocation(
        JavaMethodKey target,
        JavaDispatchKind dispatch,
        JavaInvocationArity arity,
        JavaExecutableType selectedType,
        List<JavaType> argumentTypes,
        JavaType resultType,
        List<JavaConversion> conversions) {
    public JavaInvocation {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(dispatch, "dispatch");
        Objects.requireNonNull(arity, "arity");
        Objects.requireNonNull(selectedType, "selectedType");
        argumentTypes = List.copyOf(argumentTypes);
        Objects.requireNonNull(resultType, "resultType");
        conversions = List.copyOf(conversions);
    }
}
