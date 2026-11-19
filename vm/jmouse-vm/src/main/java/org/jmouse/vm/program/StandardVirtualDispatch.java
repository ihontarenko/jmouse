package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** JVM method selection over physical identities, after symbolic resolution (JVMS 5.4.5–5.4.6). */
public final class StandardVirtualDispatch implements VirtualDispatchStrategy {

    private final ProgramHierarchy hierarchy;
    private final RuntimePackageStrategy packages;

    public StandardVirtualDispatch(ProgramHierarchy hierarchy, RuntimePackageStrategy packages) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        this.packages = Objects.requireNonNull(packages, "packages");
    }

    @Override
    public Optional<ExecutableMethodKey> select(
            JavaTypeKey receiver, ExecutableMethodKey resolvedMethod) {
        ProgramMethodDeclaration resolved =
                hierarchy
                        .declarations()
                        .findMethod(resolvedMethod)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unresolved invocation target: " + resolvedMethod));
        if (resolved.modifiers().contains(JavaStandardModifier.STATIC)
                || resolvedMethod.name().equals(JavaJvmNames.CONSTRUCTOR)
                || resolvedMethod.name().equals(JavaJvmNames.CLASS_INITIALIZER)) {
            throw new IllegalArgumentException(
                    "Virtual selection requires an instance method: " + resolvedMethod);
        }
        if (!hierarchy.isSubtype(receiver, resolvedMethod.owner())) {
            throw new IllegalArgumentException(
                    "Receiver is not a subtype of the resolved owner: " + receiver);
        }
        if (resolved.modifiers().contains(JavaStandardModifier.PRIVATE)) {
            return Optional.of(resolvedMethod);
        }
        List<ProgramMethodDeclaration> overriding = new ArrayList<>();
        overriding.add(resolved);
        ExecutableMethodKey selected = null;
        for (ProgramClassDeclaration declaration : hierarchy.classChain(receiver).reversed()) {
            if (hierarchy.declarations().requireType(resolvedMethod.owner()).kind() == ProgramClassKind.CLASS
                    && !hierarchy.isSubtype(declaration.type(), resolvedMethod.owner())) {
                continue;
            }
            ProgramMethodDeclaration candidate = matching(declaration, resolvedMethod);
            if (candidate == null || candidate.modifiers().contains(JavaStandardModifier.PRIVATE)) {
                continue;
            }
            if (overriding.stream()
                    .anyMatch(ancestor -> canDirectlyOverride(candidate, ancestor))) {
                overriding.add(candidate);
                selected = candidate.key();
            }
        }
        if (selected != null) {
            return Optional.of(selected);
        }
        return selectDefault(receiver, resolvedMethod);
    }

    private boolean canDirectlyOverride(
            ProgramMethodDeclaration candidate, ProgramMethodDeclaration ancestor) {
        return ancestor.modifiers().contains(JavaStandardModifier.PUBLIC)
                || ancestor.modifiers().contains(JavaStandardModifier.PROTECTED)
                || packages.samePackage(candidate.key().owner(), ancestor.key().owner());
    }

    private Optional<ExecutableMethodKey> selectDefault(
            JavaTypeKey receiver, ExecutableMethodKey resolved) {
        List<ProgramMethodDeclaration> declarations = new ArrayList<>();
        for (JavaTypeKey type : hierarchy.allInterfaces(receiver)) {
            ProgramMethodDeclaration candidate =
                    matching(hierarchy.declarations().requireType(type), resolved);
            if (candidate != null
                    && !candidate.modifiers().contains(JavaStandardModifier.PRIVATE)) {
                declarations.add(candidate);
            }
        }
        Set<ExecutableMethodKey> defaults = new HashSet<>();
        for (ProgramMethodDeclaration candidate : declarations) {
            boolean masked =
                    declarations.stream()
                            .anyMatch(
                                    other ->
                                            !other.key().equals(candidate.key())
                                                    && hierarchy.isSubtype(
                                                            other.key().owner(),
                                                            candidate.key().owner()));
            if (!masked && !candidate.modifiers().contains(JavaStandardModifier.ABSTRACT)) {
                defaults.add(candidate.key());
            }
        }
        if (defaults.size() > 1) {
            throw new VirtualDispatchException(receiver, resolved, defaults);
        }
        return defaults.stream().findFirst();
    }

    private static ProgramMethodDeclaration matching(
            ProgramClassDeclaration owner, ExecutableMethodKey resolved) {
        return owner.methods().stream()
                .filter(
                        method ->
                                method.key().name().equals(resolved.name())
                                        && method.key().descriptor().equals(resolved.descriptor())
                                        && !method.modifiers()
                                                .contains(JavaStandardModifier.STATIC))
                .findFirst()
                .orElse(null);
    }
}
