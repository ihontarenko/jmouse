package org.jmouse.java.model.type;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record JavaDeclaredType(JavaTypeKey identity, List<JavaType> arguments, JavaType enclosing)
        implements JavaType {
    public JavaDeclaredType {
        Objects.requireNonNull(identity, "identity");
        arguments = List.copyOf(arguments);
        Objects.requireNonNull(enclosing, "enclosing");
    }

    @Override
    public String displayName() {
        return identity.binaryName()
                + (arguments.isEmpty()
                        ? ""
                        : arguments.stream()
                                .map(JavaType::displayName)
                                .collect(Collectors.joining(", ", "<", ">")));
    }
}
