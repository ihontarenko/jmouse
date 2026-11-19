package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.symbol.JavaModifier;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Physical declaration and, where known, its explicit source-declaration correspondence. */
public record ProgramMethodDeclaration(
        ExecutableMethodKey key,
        Set<JavaModifier> modifiers,
        Optional<JavaMethodKey> declaredMethod) {

    public ProgramMethodDeclaration {
        Objects.requireNonNull(key, "key");
        modifiers = Set.copyOf(modifiers);
        Objects.requireNonNull(declaredMethod, "declaredMethod");
        declaredMethod.ifPresent(
                declared -> {
                    if (!declared.owner().equals(key.owner())
                            || !declared.name().equals(key.name())) {
                        throw new IllegalArgumentException(
                                "Source and physical method owners/names differ");
                    }
                });
    }
}
