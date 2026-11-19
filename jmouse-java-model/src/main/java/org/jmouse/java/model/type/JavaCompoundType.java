package org.jmouse.java.model.type;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record JavaCompoundType(Kind kind, List<JavaType> types) implements JavaType {
    public enum Kind {
        INTERSECTION,
        UNION
    }

    public JavaCompoundType {
        Objects.requireNonNull(kind, "kind");
        types = List.copyOf(types);
        if (types.isEmpty()) {
            throw new IllegalArgumentException("A compound type requires components");
        }
    }

    @Override
    public String displayName() {
        return types.stream()
                .map(JavaType::displayName)
                .collect(Collectors.joining(kind == Kind.INTERSECTION ? " & " : " | "));
    }
}
