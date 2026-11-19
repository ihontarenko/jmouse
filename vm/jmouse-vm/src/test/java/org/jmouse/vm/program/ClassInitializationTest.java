package org.jmouse.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jmouse.el.InputUnit;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaVoidType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class ClassInitializationTest {

    @Test
    void ordersSuperclassAndDefaultDeclaringInterfacesWithoutInitializingOtherInterfaces() {
        ProgramClassDeclaration base =
                type("Base", ProgramClassKind.CLASS, Optional.empty(), List.of(), false);
        ProgramClassDeclaration root =
                type("Root", ProgramClassKind.INTERFACE, Optional.empty(), List.of(), true);
        ProgramClassDeclaration left =
                type(
                        "Left",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(root.type()),
                        true);
        ProgramClassDeclaration right =
                type(
                        "Right",
                        ProgramClassKind.INTERFACE,
                        Optional.empty(),
                        List.of(root.type()),
                        true);
        ProgramClassDeclaration plain =
                type("Plain", ProgramClassKind.INTERFACE, Optional.empty(), List.of(), false);
        ProgramClassDeclaration receiver =
                type(
                        "Receiver",
                        ProgramClassKind.CLASS,
                        Optional.of(base.type()),
                        List.of(left.type(), plain.type(), right.type()),
                        false);
        StandardClassInitialization initialization =
                new StandardClassInitialization(
                        new ProgramHierarchy(
                                ProgramDeclarationSnapshot.assemble(
                                        List.of(base, root, left, right, plain, receiver))));

        ClassInitializationPlan plan = initialization.plan(receiver.type());

        assertEquals(
                List.of(base.type(), root.type(), left.type(), right.type()), plan.prerequisites());
        assertEquals(JavaJvmNames.CLASS_INITIALIZER, plan.initializer().orElseThrow().name());
        assertEquals(List.of(), initialization.plan(left.type()).prerequisites());
    }

    private static ProgramClassDeclaration type(
            String name,
            ProgramClassKind kind,
            Optional<JavaTypeKey> superclass,
            List<JavaTypeKey> interfaces,
            boolean declaresDefault) {
        JavaTypeKey type = new JavaTypeKey(new JavaModuleKey(""), name);
        ProgramInput input = new ProgramInput(ProgramInputKind.JVM_CLASSFILE, name, "fixture");
        ClassDefinitionOrigin origin =
                new ClassDefinitionOrigin(input, new ProgramOrigin(name, InputUnit.BYTE, 0, 10));
        ProgramMethodDeclaration initializer =
                new ProgramMethodDeclaration(
                        new ExecutableMethodKey(
                                type,
                                JavaJvmNames.CLASS_INITIALIZER,
                                new JavaMethodDescriptor(List.of(), JavaVoidType.VOID)),
                        Set.of(JavaStandardModifier.STATIC),
                        Optional.empty());
        ProgramMethodDeclaration defaultMethod =
                new ProgramMethodDeclaration(
                        new ExecutableMethodKey(
                                type,
                                "call",
                                new JavaMethodDescriptor(List.of(), JavaVoidType.VOID)),
                        Set.of(JavaStandardModifier.PUBLIC),
                        Optional.empty());
        return new ProgramClassDeclaration(
                type,
                origin,
                kind,
                Set.of(),
                superclass,
                interfaces,
                declaresDefault ? List.of(initializer, defaultMethod) : List.of(initializer),
                List.of());
    }
}
