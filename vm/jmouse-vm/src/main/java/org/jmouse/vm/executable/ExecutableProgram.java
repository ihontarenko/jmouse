package org.jmouse.vm.executable;

import org.jmouse.java.model.symbol.JavaModifier;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.program.ClassAuthority;
import org.jmouse.vm.program.ClassAuthorityException;
import org.jmouse.vm.program.ClassDefinitionOrigin;
import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramClassDeclaration;
import org.jmouse.vm.program.ProgramClassKind;
import org.jmouse.vm.program.ProgramDeclarationSnapshot;
import org.jmouse.vm.program.ProgramMethodDeclaration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Shared program and implementation selection. Each instance is an isolated immutable snapshot;
 * successful construction does not imply all reachable bodies are present or linking has passed.
 */
public final class ExecutableProgram {

    private final ProgramDeclarationSnapshot declarations;
    private final Map<JavaTypeKey, ExecutableClassBinding> classes;

    private ExecutableProgram(
            ProgramDeclarationSnapshot declarations,
            Map<JavaTypeKey, ExecutableClassBinding> classes) {
        this.declarations = declarations;
        this.classes = Map.copyOf(classes);
    }

    public static ExecutableProgram assemble(Collection<ExecutableClassBinding> candidates) {
        return assemble(candidates, ClassAuthority.unique());
    }

    public static ExecutableProgram assemble(
            Collection<ExecutableClassBinding> candidates, ClassAuthority authority) {
        Map<JavaTypeKey, Map<ClassDefinitionOrigin, ExecutableClassBinding>> grouped =
                new LinkedHashMap<>();
        for (ExecutableClassBinding candidate : candidates) {
            Map<ClassDefinitionOrigin, ExecutableClassBinding> definitions =
                    grouped.computeIfAbsent(
                            candidate.declaration().type(), ignored -> new LinkedHashMap<>());
            ExecutableClassBinding previous =
                    definitions.putIfAbsent(candidate.declaration().origin(), candidate);
            if (previous != null && !previous.equals(candidate)) {
                throw new ClassAuthorityException(
                        candidate.declaration().type(),
                        List.copyOf(definitions.keySet()),
                        "One input origin supplies conflicting executable implementations");
            }
        }
        List<ProgramClassDeclaration> headers =
                grouped.values().stream()
                        .flatMap(definitions -> definitions.values().stream())
                        .map(ExecutableClassBinding::declaration)
                        .toList();
        ProgramDeclarationSnapshot declarations =
                ProgramDeclarationSnapshot.assemble(headers, authority);
        Map<JavaTypeKey, ExecutableClassBinding> selected = new HashMap<>();
        for (JavaTypeKey type : declarations.types()) {
            ClassDefinitionOrigin origin = declarations.requireType(type).origin();
            selected.put(type, grouped.get(type).get(origin));
        }
        return new ExecutableProgram(declarations, selected);
    }

    public ProgramDeclarationSnapshot declarations() {
        return declarations;
    }

    public Set<JavaTypeKey> types() {
        return classes.keySet();
    }

    public ExecutableClassBinding requireClass(JavaTypeKey type) {
        ExecutableClassBinding selected = classes.get(Objects.requireNonNull(type, "type"));
        if (selected == null) {
            throw new IllegalArgumentException("Missing executable class binding: " + type);
        }
        return selected;
    }

    public Optional<ExecutableMethodBinding> findMethod(ExecutableMethodKey method) {
        Objects.requireNonNull(method, "method");
        ExecutableClassBinding selected = classes.get(method.owner());
        return selected == null ? Optional.empty() : selected.methods().find(method);
    }

    public ExecutableMethodBinding requireMethod(ExecutableMethodKey method) {
        return findMethod(method).orElseThrow(() -> new MissingExecutableBodyException(method));
    }

    public ExecutableProgram withMethodReplacement(
            ExecutableMethodKey method,
            ImplementationIdentity expected,
            ExecutableMethodBinding replacement) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(replacement, "replacement");
        ExecutableClassBinding current = requireClass(method.owner());
        ProgramMethodDeclaration declared =
                declarations
                        .findMethod(method)
                        .orElseThrow(() -> new MissingExecutableBodyException(method));
        if (!method.equals(replacement.method())) {
            throw new IllegalArgumentException(
                    "A replacement must preserve the physical method identity");
        }
        ExecutableBodyBindings replaced;
        if (current.methods().find(method).isPresent()) {
            replaced = current.methods().withReplacement(method, expected, replacement);
        } else {
            if (!current.identity().equals(expected)) {
                throw new ImplementationBindingException(expected, current.identity());
            }
            replaced = current.methods().withBinding(replacement);
        }
        ProgramClassDeclaration implemented =
                implementedDeclaration(current.declaration(), declared);
        Set<ExecutableMethodKey> replacements = new HashSet<>(current.methodReplacements());
        replacements.add(method);
        return withClassBinding(
                new ExecutableClassBinding(
                        current.identity(), implemented, replaced, replacements));
    }

    public ExecutableProgram withClassReplacement(
            JavaTypeKey type, ImplementationIdentity expected, ExecutableClassBinding replacement) {
        return withClassReplacement(
                type, expected, replacement, ClassReplacementPolicy.REQUIRE_UNMODIFIED);
    }

    public ExecutableProgram withClassReplacement(
            JavaTypeKey type,
            ImplementationIdentity expected,
            ExecutableClassBinding replacement,
            ClassReplacementPolicy policy) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(replacement, "replacement");
        Objects.requireNonNull(policy, "policy");
        ExecutableClassBinding current = requireClass(type);
        if (!type.equals(replacement.declaration().type())) {
            throw new IllegalArgumentException(
                    "A replacement must preserve the logical type identity");
        }
        if (!expected.equals(current.identity())) {
            throw new ImplementationBindingException(expected, current.identity());
        }
        if (policy == ClassReplacementPolicy.REQUIRE_UNMODIFIED
                && !current.methodReplacements().isEmpty()) {
            throw new ClassReplacementConflictException(type, current.methodReplacements());
        }
        ExecutableProgram selected = withClassBinding(replacement);
        if (policy == ClassReplacementPolicy.PRESERVE_METHOD_REPLACEMENTS) {
            for (ExecutableMethodKey method : current.methodReplacements()) {
                ExecutableMethodBinding body = current.methods().require(method);
                ImplementationIdentity previous =
                        selected.findMethod(method)
                                .map(ExecutableMethodBinding::identity)
                                .orElse(replacement.identity());
                selected = selected.withMethodReplacement(method, previous, body);
            }
        }
        return selected;
    }

    private ExecutableProgram withClassBinding(ExecutableClassBinding replacement) {
        Map<JavaTypeKey, ExecutableClassBinding> classes = new HashMap<>(this.classes);
        classes.put(replacement.declaration().type(), replacement);
        List<ProgramClassDeclaration> headers =
                classes.values().stream().map(ExecutableClassBinding::declaration).toList();
        return new ExecutableProgram(ProgramDeclarationSnapshot.assemble(headers), classes);
    }

    private static ProgramClassDeclaration implementedDeclaration(
            ProgramClassDeclaration owner, ProgramMethodDeclaration declared) {
        Set<JavaModifier> modifiers = new HashSet<>(declared.modifiers());
        modifiers.remove(JavaStandardModifier.ABSTRACT);
        modifiers.remove(JavaStandardModifier.NATIVE);
        if (owner.kind() == ProgramClassKind.INTERFACE
                && !modifiers.contains(JavaStandardModifier.STATIC)
                && !modifiers.contains(JavaStandardModifier.PRIVATE)) {
            modifiers.add(JavaStandardModifier.DEFAULT);
        }
        ProgramMethodDeclaration implemented =
                new ProgramMethodDeclaration(declared.key(), modifiers, declared.declaredMethod());
        List<ProgramMethodDeclaration> methods =
                owner.methods().stream()
                        .map(method -> method.key().equals(declared.key()) ? implemented : method)
                        .toList();
        return new ProgramClassDeclaration(
                owner.type(),
                owner.origin(),
                owner.kind(),
                owner.modifiers(),
                owner.superclass(),
                owner.interfaces(),
                methods,
                owner.fields());
    }
}
