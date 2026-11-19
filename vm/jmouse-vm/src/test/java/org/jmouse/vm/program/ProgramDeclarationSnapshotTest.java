package org.jmouse.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.el.InputUnit;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaVoidType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class ProgramDeclarationSnapshotTest {

    private static final JavaTypeKey TYPE =
            new JavaTypeKey(new JavaModuleKey(""), "example.Sample");

    @Test
    void selectsTheWholeClassAndPreservesPhysicalReturnOnlyOverloads() {
        ProgramClassDeclaration source =
                declaration(ProgramInputKind.JAVA_SOURCE, List.of("sourceOnly"));
        ProgramClassDeclaration binary =
                declaration(ProgramInputKind.JVM_CLASSFILE, List.of("binaryOnly"));

        assertThrows(
                ClassAuthorityException.class,
                () -> ProgramDeclarationSnapshot.assemble(List.of(source, binary)));
        ProgramDeclarationSnapshot snapshot =
                ProgramDeclarationSnapshot.assemble(
                        List.of(source, binary), (type, candidates) -> binary.origin());

        assertEquals(binary, snapshot.requireType(TYPE));
        assertEquals(
                Set.of("binaryOnly"),
                snapshot.requireType(TYPE).methods().stream()
                        .map(method -> method.key().name())
                        .collect(Collectors.toSet()));
        assertEquals(2, snapshot.requireType(TYPE).methods().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.types().clear());
    }

    @Test
    void refusesAnAuthorityChoiceOutsideTheCandidates() {
        ProgramClassDeclaration source =
                declaration(ProgramInputKind.JAVA_SOURCE, List.of("sourceOnly"));
        ProgramClassDeclaration binary =
                declaration(ProgramInputKind.JVM_CLASSFILE, List.of("binaryOnly"));

        assertThrows(
                ClassAuthorityException.class,
                () ->
                        ProgramDeclarationSnapshot.assemble(
                                List.of(source), (type, candidates) -> binary.origin()));
    }

    private static ProgramClassDeclaration declaration(ProgramInputKind kind, List<String> names) {
        ProgramInput input = new ProgramInput(kind, kind.name(), "fixture-content-digest");
        ClassDefinitionOrigin origin =
                new ClassDefinitionOrigin(
                        input,
                        new ProgramOrigin(
                                kind.name(),
                                kind == ProgramInputKind.JAVA_SOURCE
                                        ? InputUnit.UTF16_CODE_UNIT
                                        : InputUnit.BYTE,
                                0,
                                10));
        List<ProgramMethodDeclaration> methods =
                List.of(
                        new ProgramMethodDeclaration(
                                new ExecutableMethodKey(
                                        TYPE,
                                        names.getFirst(),
                                        new JavaMethodDescriptor(List.of(), JavaVoidType.VOID)),
                                Set.of(),
                                Optional.empty()),
                        new ProgramMethodDeclaration(
                                new ExecutableMethodKey(
                                        TYPE,
                                        names.getFirst(),
                                        new JavaMethodDescriptor(List.of(), JavaPrimitiveType.INT)),
                                Set.of(),
                                Optional.empty()));
        return new ProgramClassDeclaration(
                TYPE,
                origin,
                ProgramClassKind.CLASS,
                Set.of(),
                Optional.empty(),
                List.of(),
                methods,
                List.of());
    }
}
