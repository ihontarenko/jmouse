package org.jmouse.vm.executable;

import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramClassDeclaration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** One class candidate and its available bodies. Missing/native implementations remain explicit. */
public record ExecutableClassBinding(
        ImplementationIdentity identity,
        ProgramClassDeclaration declaration,
        ExecutableBodyBindings methods,
        Set<ExecutableMethodKey> methodReplacements) {

    public ExecutableClassBinding(
            ImplementationIdentity identity,
            ProgramClassDeclaration declaration,
            ExecutableBodyBindings methods) {
        this(identity, declaration, methods, Set.of());
    }

    public ExecutableClassBinding {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(methods, "methods");
        methodReplacements = Set.copyOf(methodReplacements);
        Set<ExecutableMethodKey> declared =
                declaration.methods().stream()
                        .map(method -> method.key())
                        .collect(Collectors.toUnmodifiableSet());
        for (ExecutableMethodBinding method : methods.bindings()) {
            if (!declared.contains(method.method())) {
                throw new IllegalArgumentException(
                        "Body does not belong to this class declaration: " + method.method());
            }
        }
        for (ExecutableMethodKey method : methodReplacements) {
            if (methods.find(method).isEmpty()) {
                throw new IllegalArgumentException(
                        "Method replacement has no executable binding: " + method);
            }
        }
    }
}
