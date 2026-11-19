package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaFieldKey;
import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Authoritative immutable declarations shared by source and binary executable frontends. */
public final class ProgramDeclarationSnapshot {

    private final Map<JavaTypeKey, ProgramClassDeclaration> classes;
    private final Map<ExecutableMethodKey, ProgramMethodDeclaration> methods;
    private final Map<JavaFieldKey, ProgramFieldDeclaration> fields;

    private ProgramDeclarationSnapshot(Map<JavaTypeKey, ProgramClassDeclaration> classes) {
        this.classes = Map.copyOf(classes);
        Map<ExecutableMethodKey, ProgramMethodDeclaration> methodDeclarations = new HashMap<>();
        Map<JavaFieldKey, ProgramFieldDeclaration> fieldDeclarations = new HashMap<>();
        for (ProgramClassDeclaration declaration : classes.values()) {
            for (ProgramMethodDeclaration method : declaration.methods()) {
                methodDeclarations.put(method.key(), method);
            }
            for (ProgramFieldDeclaration field : declaration.fields()) {
                fieldDeclarations.put(field.key(), field);
            }
        }
        this.methods = Map.copyOf(methodDeclarations);
        this.fields = Map.copyOf(fieldDeclarations);
    }

    public static ProgramDeclarationSnapshot assemble(
            Collection<ProgramClassDeclaration> candidates) {
        return assemble(candidates, ClassAuthority.unique());
    }

    public static ProgramDeclarationSnapshot assemble(
            Collection<ProgramClassDeclaration> candidates, ClassAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        Map<JavaTypeKey, Map<ClassDefinitionOrigin, ProgramClassDeclaration>> grouped =
                new LinkedHashMap<>();
        for (ProgramClassDeclaration candidate : candidates) {
            Map<ClassDefinitionOrigin, ProgramClassDeclaration> definitions =
                    grouped.computeIfAbsent(candidate.type(), ignored -> new LinkedHashMap<>());
            ProgramClassDeclaration existing =
                    definitions.putIfAbsent(candidate.origin(), candidate);
            if (existing != null && !existing.equals(candidate)) {
                throw new ClassAuthorityException(
                        candidate.type(),
                        List.copyOf(definitions.keySet()),
                        "One input origin supplies conflicting declarations");
            }
        }
        Map<JavaTypeKey, ProgramClassDeclaration> selected = new HashMap<>();
        grouped.forEach(
                (type, definitions) -> {
                    List<ClassDefinitionOrigin> origins = List.copyOf(definitions.keySet());
                    ClassDefinitionOrigin origin = authority.select(type, origins);
                    ProgramClassDeclaration declaration = definitions.get(origin);
                    if (declaration == null) {
                        throw new ClassAuthorityException(
                                type,
                                origins,
                                "Authority selected an origin outside its candidates");
                    }
                    selected.put(type, declaration);
                });
        return new ProgramDeclarationSnapshot(selected);
    }

    public Set<JavaTypeKey> types() {
        return classes.keySet();
    }

    public Optional<ProgramClassDeclaration> findType(JavaTypeKey type) {
        return Optional.ofNullable(classes.get(Objects.requireNonNull(type, "type")));
    }

    public ProgramClassDeclaration requireType(JavaTypeKey type) {
        return findType(type)
                .orElseThrow(() -> new IllegalArgumentException("Missing program class: " + type));
    }

    public Optional<ProgramMethodDeclaration> findMethod(ExecutableMethodKey method) {
        return Optional.ofNullable(methods.get(Objects.requireNonNull(method, "method")));
    }

    public Optional<ProgramFieldDeclaration> findField(JavaFieldKey field) {
        return Optional.ofNullable(fields.get(Objects.requireNonNull(field, "field")));
    }
}
