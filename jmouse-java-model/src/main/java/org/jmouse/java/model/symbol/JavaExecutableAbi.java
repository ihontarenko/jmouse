package org.jmouse.java.model.symbol;

import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Language signatures and JVM constructor ABI are distinct. Captured environments require an
 * explicit layout pass.
 */
public record JavaExecutableAbi(
        JavaMethodDescriptor declaredDescriptor,
        List<JavaImplicitParameter> prefix,
        JavaAbiStatus status) {
    public JavaExecutableAbi {
        Objects.requireNonNull(declaredDescriptor, "declaredDescriptor");
        prefix = List.copyOf(prefix);
        Objects.requireNonNull(status, "status");
    }

    public Optional<JavaMethodDescriptor> effectiveDescriptor() {
        if (status == JavaAbiStatus.CAPTURE_DEPENDENT) {
            return Optional.empty();
        }
        List<JavaTypeDescriptor> parameters = new ArrayList<>();
        prefix.forEach(parameter -> parameters.add(parameter.type()));
        parameters.addAll(declaredDescriptor.parameters());
        return Optional.of(new JavaMethodDescriptor(parameters, declaredDescriptor.result()));
    }
}
