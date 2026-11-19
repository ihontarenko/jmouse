package org.jmouse.java.model.descriptor;

import org.jmouse.java.model.type.JavaVoidType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record JavaMethodDescriptor(List<JavaTypeDescriptor> parameters, JavaTypeDescriptor result) {
    public JavaMethodDescriptor {
        parameters = List.copyOf(parameters);
        Objects.requireNonNull(result, "result");
        if (parameters.stream().anyMatch(parameter -> parameter == JavaVoidType.VOID)
                || parameters.stream().mapToInt(JavaTypeDescriptor::parameterSlots).sum()
                        > MAXIMUM_PARAMETER_SLOTS) {
            throw new IllegalArgumentException(
                    "Invalid JVM parameter slot count or void parameter");
        }
    }

    public static final int MAXIMUM_PARAMETER_SLOTS = 255;

    public String encoding() {
        return "("
                + parameters.stream()
                        .map(JavaTypeDescriptor::encoding)
                        .collect(Collectors.joining())
                + ")"
                + result.encoding();
    }
}
