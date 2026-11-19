package org.jmouse.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.el.InputUnit;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaModifier;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaVoidType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class VirtualDispatchTest {

    @Test
    void aClassImplementationTakesPrecedenceOverAnInterfaceDefault() {
        ProgramClassDeclaration contract =
                type(
                        "sample.Contract",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration base =
                type(
                        "sample.Base",
                        ProgramClassKind.CLASS,
                        Optional.empty(),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration receiver =
                withoutMethods(
                        type(
                                "sample.Receiver",
                                ProgramClassKind.CLASS,
                                Optional.of(base.type()),
                                List.of(contract.type()),
                                Set.of()));

        assertEquals(
                base.methods().getFirst().key(),
                dispatch(List.of(contract, base, receiver))
                        .select(receiver.type(), contract.methods().getFirst().key())
                        .orElseThrow());
    }

    @Test
    void aResolvedPrivateMethodIsNotReplacedByASameSignatureSubclassMethod() {
        ProgramClassDeclaration base =
                type(
                        "sample.Base",
                        ProgramClassKind.CLASS,
                        Optional.empty(),
                        List.of(),
                        Set.of(JavaStandardModifier.PRIVATE));
        ProgramClassDeclaration receiver =
                type(
                        "sample.Receiver",
                        ProgramClassKind.CLASS,
                        Optional.of(base.type()),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC));

        assertEquals(
                base.methods().getFirst().key(),
                dispatch(List.of(base, receiver))
                        .select(receiver.type(), base.methods().getFirst().key())
                        .orElseThrow());
    }

    @Test
    void physicalReturnOnlyOverloadsAreNeverSelectedByNameAlone() {
        ProgramClassDeclaration base =
                type(
                        "sample.Base",
                        ProgramClassKind.CLASS,
                        Optional.empty(),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration receiver =
                type(
                        "sample.Receiver",
                        ProgramClassKind.CLASS,
                        Optional.of(base.type()),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramMethodDeclaration returnOnly =
                new ProgramMethodDeclaration(
                        new ExecutableMethodKey(
                                receiver.type(),
                                "call",
                                new JavaMethodDescriptor(List.of(), JavaPrimitiveType.INT)),
                        Set.of(JavaStandardModifier.PUBLIC),
                        Optional.empty());
        ProgramClassDeclaration overloaded =
                new ProgramClassDeclaration(
                        receiver.type(),
                        receiver.origin(),
                        receiver.kind(),
                        receiver.modifiers(),
                        receiver.superclass(),
                        receiver.interfaces(),
                        List.of(returnOnly, receiver.methods().getFirst()),
                        List.of());

        assertEquals(
                receiver.methods().getFirst().key(),
                dispatch(List.of(base, overloaded))
                        .select(receiver.type(), base.methods().getFirst().key())
                        .orElseThrow());
    }

    @Test
    void cyclicInterfacesAreRefusedWithoutRecursion() {
        JavaTypeKey secondKey = new JavaTypeKey(new JavaModuleKey(""), "sample.Second");
        ProgramClassDeclaration first =
                type(
                        "sample.First",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(secondKey),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration second =
                type(
                        "sample.Second",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(first.type()),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramHierarchy hierarchy =
                new ProgramHierarchy(ProgramDeclarationSnapshot.assemble(List.of(first, second)));

        assertThrows(IllegalArgumentException.class, () -> hierarchy.allInterfaces(first.type()));
    }

    @Test
    void selectsAnOverrideAcrossPackagesThroughAnIntermediatePublicOverride() {
        ProgramClassDeclaration first =
                type("one.First", ProgramClassKind.CLASS, Optional.empty(), List.of(), Set.of());
        ProgramClassDeclaration second =
                type(
                        "one.Second",
                        ProgramClassKind.CLASS,
                        Optional.of(first.type()),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration third =
                type(
                        "one.Third",
                        ProgramClassKind.CLASS,
                        Optional.of(second.type()),
                        List.of(),
                        Set.of());
        ProgramClassDeclaration last =
                type(
                        "two.Last",
                        ProgramClassKind.CLASS,
                        Optional.of(third.type()),
                        List.of(),
                        Set.of());
        StandardVirtualDispatch dispatch = dispatch(List.of(first, second, third, last));

        assertEquals(
                last.methods().getFirst().key(),
                dispatch.select(last.type(), first.methods().getFirst().key()).orElseThrow());
        assertEquals(
                third.methods().getFirst().key(),
                dispatch.select(last.type(), third.methods().getFirst().key()).orElseThrow());
    }

    @Test
    void anAbstractSubinterfaceDeclarationMasksAnInheritedDefault() {
        ProgramClassDeclaration parent =
                type(
                        "sample.Parent",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration child =
                type(
                        "sample.Child",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(parent.type()),
                        Set.of(JavaStandardModifier.PUBLIC, JavaStandardModifier.ABSTRACT));
        ProgramClassDeclaration receiver =
                withoutMethods(
                        type(
                                "sample.Receiver",
                                ProgramClassKind.CLASS,
                                Optional.empty(),
                                List.of(child.type()),
                                Set.of()));

        assertEquals(
                Optional.empty(),
                dispatch(List.of(parent, child, receiver))
                        .select(receiver.type(), parent.methods().getFirst().key()));
    }

    @Test
    void refusesConflictingDefaultsInsteadOfChoosingByInputOrder() {
        ProgramClassDeclaration parent =
                type(
                        "sample.Parent",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(),
                        Set.of(JavaStandardModifier.PUBLIC, JavaStandardModifier.ABSTRACT));
        ProgramClassDeclaration left =
                type(
                        "sample.Left",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(parent.type()),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration right =
                type(
                        "sample.Right",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(parent.type()),
                        Set.of(JavaStandardModifier.PUBLIC));
        ProgramClassDeclaration receiver =
                withoutMethods(
                        type(
                                "sample.Receiver",
                                ProgramClassKind.CLASS,
                                Optional.empty(),
                                List.of(left.type(), right.type()),
                                Set.of()));

        assertThrows(
                VirtualDispatchException.class,
                () ->
                        dispatch(List.of(parent, left, right, receiver))
                                .select(receiver.type(), parent.methods().getFirst().key()));
    }

    @Test
    void missingAndCyclicHierarchyDefinitionsCannotLookLikeValidLeaves() {
        JavaTypeKey missing = new JavaTypeKey(new JavaModuleKey(""), "sample.Missing");
        ProgramClassDeclaration receiver =
                withoutMethods(
                        type(
                                "sample.Receiver",
                                ProgramClassKind.CLASS,
                                Optional.of(missing),
                                List.of(),
                                Set.of()));
        ProgramHierarchy hierarchy =
                new ProgramHierarchy(ProgramDeclarationSnapshot.assemble(List.of(receiver)));
        assertThrows(IllegalArgumentException.class, () -> hierarchy.classChain(receiver.type()));

        ProgramClassDeclaration cycle =
                withoutMethods(
                        type(
                                "sample.Missing",
                                ProgramClassKind.CLASS,
                                Optional.of(receiver.type()),
                                List.of(),
                                Set.of()));
        ProgramHierarchy cyclic =
                new ProgramHierarchy(ProgramDeclarationSnapshot.assemble(List.of(receiver, cycle)));
        assertThrows(IllegalArgumentException.class, () -> cyclic.classChain(receiver.type()));
    }

    private static StandardVirtualDispatch dispatch(List<ProgramClassDeclaration> classes) {
        return new StandardVirtualDispatch(
                new ProgramHierarchy(ProgramDeclarationSnapshot.assemble(classes)),
                RuntimePackageStrategy.moduleScoped());
    }

    private static ProgramClassDeclaration type(
            String name,
            ProgramClassKind kind,
            Optional<JavaTypeKey> superclass,
            List<JavaTypeKey> interfaces,
            Set<JavaModifier> methodModifiers) {
        JavaTypeKey type = new JavaTypeKey(new JavaModuleKey(""), name);
        ProgramInput input = new ProgramInput(ProgramInputKind.JVM_CLASSFILE, name, "fixture");
        ClassDefinitionOrigin origin =
                new ClassDefinitionOrigin(input, new ProgramOrigin(name, InputUnit.BYTE, 0, 10));
        ProgramMethodDeclaration method =
                new ProgramMethodDeclaration(
                        new ExecutableMethodKey(
                                type,
                                "call",
                                new JavaMethodDescriptor(List.of(), JavaVoidType.VOID)),
                        methodModifiers,
                        Optional.empty());
        return new ProgramClassDeclaration(
                type, origin, kind, Set.of(), superclass, interfaces, List.of(method), List.of());
    }

    private static ProgramClassDeclaration withoutMethods(ProgramClassDeclaration declaration) {
        return new ProgramClassDeclaration(
                declaration.type(),
                declaration.origin(),
                declaration.kind(),
                declaration.modifiers(),
                declaration.superclass(),
                declaration.interfaces(),
                List.of(),
                List.of());
    }
}
