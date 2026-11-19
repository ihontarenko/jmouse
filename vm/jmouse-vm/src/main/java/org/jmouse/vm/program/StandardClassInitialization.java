package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Static prerequisites for Java class initialization (JLS 12.4.2 / JVMS 5.5). */
public final class StandardClassInitialization implements ClassInitializationStrategy {

    private final ProgramHierarchy hierarchy;

    public StandardClassInitialization(ProgramHierarchy hierarchy) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
    }

    @Override
    public ClassInitializationPlan plan(JavaTypeKey type) {
        ProgramClassDeclaration declaration = hierarchy.declarations().requireType(type);
        hierarchy.classChain(type);
        List<JavaTypeKey> interfaces = hierarchy.declaredInterfaceClosure(type);
        List<JavaTypeKey> prerequisites = new ArrayList<>();
        if (declaration.kind() == ProgramClassKind.CLASS) {
            declaration.superclass().ifPresent(prerequisites::add);
            for (JavaTypeKey parent : interfaces) {
                if (declaresDefaultMethod(hierarchy.declarations().requireType(parent))) {
                    prerequisites.add(parent);
                }
            }
        }
        List<ExecutableMethodKey> initializers =
                declaration.methods().stream()
                        .map(ProgramMethodDeclaration::key)
                        .filter(method -> method.name().equals(JavaJvmNames.CLASS_INITIALIZER))
                        .toList();
        if (initializers.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple class initializer candidates: " + initializers);
        }
        Optional<ExecutableMethodKey> initializer = initializers.stream().findFirst();
        return new ClassInitializationPlan(type, prerequisites, initializer);
    }

    private static boolean declaresDefaultMethod(ProgramClassDeclaration declaration) {
        return declaration.methods().stream()
                .anyMatch(
                        method ->
                                !method.modifiers().contains(JavaStandardModifier.STATIC)
                                        && !method.modifiers()
                                                .contains(JavaStandardModifier.PRIVATE)
                                        && !method.modifiers()
                                                .contains(JavaStandardModifier.ABSTRACT)
                                        && !method.key()
                                                .name()
                                                .equals(JavaJvmNames.CLASS_INITIALIZER));
    }
}
