package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaConstantValue;
import org.jmouse.java.model.symbol.JavaFieldKey;
import org.jmouse.java.model.symbol.JavaModifier;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ProgramFieldDeclaration(
        JavaFieldKey key, Set<JavaModifier> modifiers, Optional<JavaConstantValue> constant) {

    public ProgramFieldDeclaration {
        Objects.requireNonNull(key, "key");
        modifiers = Set.copyOf(modifiers);
        Objects.requireNonNull(constant, "constant");
    }
}
