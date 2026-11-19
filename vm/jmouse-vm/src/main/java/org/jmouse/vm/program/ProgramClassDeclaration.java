package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaFieldKey;
import org.jmouse.java.model.symbol.JavaModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** One complete declaration candidate. Bodies are imported and validated by separate frontends. */
public record ProgramClassDeclaration(
        JavaTypeKey type,
        ClassDefinitionOrigin origin,
        ProgramClassKind kind,
        Set<JavaModifier> modifiers,
        Optional<JavaTypeKey> superclass,
        List<JavaTypeKey> interfaces,
        List<ProgramMethodDeclaration> methods,
        List<ProgramFieldDeclaration> fields) {

    public ProgramClassDeclaration {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(kind, "kind");
        modifiers = Set.copyOf(modifiers);
        Objects.requireNonNull(superclass, "superclass");
        interfaces = List.copyOf(interfaces);
        methods = List.copyOf(methods);
        fields = List.copyOf(fields);
        if (interfaces.contains(type)
                || superclass.filter(type::equals).isPresent()
                || new HashSet<>(interfaces).size() != interfaces.size()) {
            throw new IllegalArgumentException("Invalid direct supertypes for " + type);
        }
        Set<ExecutableMethodKey> methodKeys = new HashSet<>();
        for (ProgramMethodDeclaration method : methods) {
            if (!type.equals(method.key().owner()) || !methodKeys.add(method.key())) {
                throw new IllegalArgumentException("Duplicate or foreign method: " + method.key());
            }
        }
        Set<JavaFieldKey> fieldKeys = new HashSet<>();
        for (ProgramFieldDeclaration field : fields) {
            if (!type.equals(field.key().owner()) || !fieldKeys.add(field.key())) {
                throw new IllegalArgumentException("Duplicate or foreign field: " + field.key());
            }
        }
    }
}
