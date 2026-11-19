package org.jmouse.vm.program;

import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaExecutableAbi;
import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.Objects;

/**
 * Physical method identity: includes the return descriptor and every implicit JVM parameter. This
 * is deliberately distinct from an as-declared source method and its unresolved capture ABI.
 */
public record ExecutableMethodKey(JavaTypeKey owner, String name, JavaMethodDescriptor descriptor) {

    public ExecutableMethodKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(descriptor, "descriptor");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("An executable method requires a name");
        }
    }

    public static ExecutableMethodKey fromDeclaration(
            JavaMethodKey declared, JavaExecutableAbi abi) {
        Objects.requireNonNull(declared, "declared");
        Objects.requireNonNull(abi, "abi");
        if (!declared.descriptor().equals(abi.declaredDescriptor())) {
            throw new IllegalArgumentException(
                    "ABI does not describe this source method: " + declared);
        }
        JavaMethodDescriptor physical =
                abi.effectiveDescriptor()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Capture layout is unresolved: " + declared));
        return new ExecutableMethodKey(declared.owner(), declared.name(), physical);
    }
}
