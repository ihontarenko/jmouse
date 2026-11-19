package org.jmouse.java.model.type;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record JavaExecutableType(
        List<JavaType> parameters,
        JavaType result,
        List<JavaType> thrown,
        List<JavaTypeVariable> variables,
        JavaType receiver)
        implements JavaType {
    public JavaExecutableType {
        parameters = List.copyOf(parameters);
        Objects.requireNonNull(result, "result");
        thrown = List.copyOf(thrown);
        variables = List.copyOf(variables);
        Objects.requireNonNull(receiver, "receiver");
    }

    @Override
    public String displayName() {
        return parameters.stream()
                        .map(JavaType::displayName)
                        .collect(Collectors.joining(", ", "(", ")"))
                + result.displayName();
    }
}
