package org.jmouse.vm.graph;

import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramClassDeclaration;
import org.jmouse.vm.program.ProgramClassKind;
import org.jmouse.vm.program.ProgramHierarchy;

import java.util.Objects;

/**
 * Conservative candidate types. Externally constructed objects need explicit instantiation roots.
 */
@FunctionalInterface
public interface ReceiverTypeStrategy {

    Iterable<JavaTypeKey> candidates(ExecutableMethodKey resolvedMethod);

    /** All concrete subtypes in the supplied, closed declaration universe. */
    static ReceiverTypeStrategy closedWorld(ProgramHierarchy hierarchy) {
        Objects.requireNonNull(hierarchy, "hierarchy");
        return resolvedMethod ->
                () ->
                        hierarchy.declarations().types().stream()
                                .filter(
                                        type -> {
                                            ProgramClassDeclaration declaration =
                                                    hierarchy.declarations().requireType(type);
                                            return declaration.kind() == ProgramClassKind.CLASS
                                                    && !declaration
                                                            .modifiers()
                                                            .contains(JavaStandardModifier.ABSTRACT)
                                                    && hierarchy.isSubtype(
                                                            type, resolvedMethod.owner());
                                        })
                                .iterator();
    }
}
