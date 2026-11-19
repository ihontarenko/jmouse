package org.jmouse.vm.executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.el.InputUnit;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaVoidType;
import org.jmouse.vm.executable.node.ExecutableBlockNode;
import org.jmouse.vm.program.ClassDefinitionOrigin;
import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramClassDeclaration;
import org.jmouse.vm.program.ProgramClassKind;
import org.jmouse.vm.program.ProgramHierarchy;
import org.jmouse.vm.program.ProgramInput;
import org.jmouse.vm.program.ProgramInputKind;
import org.jmouse.vm.program.ProgramMethodDeclaration;
import org.jmouse.vm.program.ProgramOrigin;
import org.jmouse.vm.program.RuntimePackageStrategy;
import org.jmouse.vm.program.StandardVirtualDispatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class ExecutableProgramTest {

    private static final JavaTypeKey TYPE =
            new JavaTypeKey(new JavaModuleKey(""), "sample.Example");
    private static final ImplementationIdentity STANDARD =
            new ImplementationIdentity("fixture.standard", "1");
    private static final ImplementationIdentity CUSTOM =
            new ImplementationIdentity("fixture.custom", "1");

    @Test
    void anInterfaceMethodReplacementUpdatesDispatchOnlyInItsProject() {
        ExecutableClassBinding template = classBinding(STANDARD, "run");
        ProgramClassDeclaration original = template.declaration();
        ProgramMethodDeclaration abstractMethod =
                new ProgramMethodDeclaration(
                        method("run"),
                        Set.of(JavaStandardModifier.PUBLIC, JavaStandardModifier.ABSTRACT),
                        Optional.empty());
        ProgramClassDeclaration contract =
                new ProgramClassDeclaration(
                        TYPE,
                        original.origin(),
                        ProgramClassKind.INTERFACE,
                        Set.of(JavaStandardModifier.ABSTRACT),
                        Optional.empty(),
                        List.of(),
                        List.of(abstractMethod),
                        List.of());
        JavaTypeKey receiver = new JavaTypeKey(TYPE.module(), "sample.Receiver");
        ProgramClassDeclaration implementation =
                new ProgramClassDeclaration(
                        receiver,
                        original.origin(),
                        ProgramClassKind.CLASS,
                        Set.of(),
                        Optional.empty(),
                        List.of(TYPE),
                        List.of(),
                        List.of());
        ExecutableProgram baseline =
                ExecutableProgram.assemble(
                        List.of(
                                new ExecutableClassBinding(
                                        STANDARD, contract, ExecutableBodyBindings.of(List.of())),
                                new ExecutableClassBinding(
                                        STANDARD,
                                        implementation,
                                        ExecutableBodyBindings.of(List.of()))));
        ExecutableProgram customized =
                baseline.withMethodReplacement(
                        method("run"),
                        STANDARD,
                        new ExecutableMethodBinding(CUSTOM, body(method("run"), CUSTOM)));
        StandardVirtualDispatch originalDispatch =
                new StandardVirtualDispatch(
                        new ProgramHierarchy(baseline.declarations()),
                        RuntimePackageStrategy.moduleScoped());
        StandardVirtualDispatch customDispatch =
                new StandardVirtualDispatch(
                        new ProgramHierarchy(customized.declarations()),
                        RuntimePackageStrategy.moduleScoped());

        assertEquals(Optional.empty(), originalDispatch.select(receiver, method("run")));
        assertEquals(Optional.of(method("run")), customDispatch.select(receiver, method("run")));
    }

    @Test
    void aClassOverrideCannotSilentlyEraseAnEarlierMethodOverride() {
        ExecutableProgram baseline =
                ExecutableProgram.assemble(List.of(classBinding(STANDARD, "run")));
        ExecutableMethodKey method = method("run");
        ExecutableProgram methodOverride =
                baseline.withMethodReplacement(
                        method,
                        STANDARD,
                        new ExecutableMethodBinding(CUSTOM, body(method, CUSTOM)));
        ImplementationIdentity other = new ImplementationIdentity("fixture.other-class", "1");
        ExecutableClassBinding replacement = classBinding(other, "run");

        assertThrows(
                ClassReplacementConflictException.class,
                () -> methodOverride.withClassReplacement(TYPE, STANDARD, replacement));
        ExecutableProgram preserved =
                methodOverride.withClassReplacement(
                        TYPE,
                        STANDARD,
                        replacement,
                        ClassReplacementPolicy.PRESERVE_METHOD_REPLACEMENTS);
        ExecutableProgram discarded =
                methodOverride.withClassReplacement(
                        TYPE,
                        STANDARD,
                        replacement,
                        ClassReplacementPolicy.DISCARD_METHOD_REPLACEMENTS);

        assertEquals(CUSTOM, preserved.requireMethod(method).identity());
        assertEquals(other, discarded.requireMethod(method).identity());
        assertEquals(CUSTOM, methodOverride.requireMethod(method).identity());
    }

    @Test
    void aNativeDeclarationCanReceiveAProjectLocalExecutableImplementation() {
        ExecutableClassBinding ordinary = classBinding(STANDARD, "run");
        ProgramClassDeclaration original = ordinary.declaration();
        ProgramMethodDeclaration nativeMethod =
                new ProgramMethodDeclaration(
                        method("run"), Set.of(JavaStandardModifier.NATIVE), Optional.empty());
        ProgramClassDeclaration nativeClass =
                new ProgramClassDeclaration(
                        original.type(),
                        original.origin(),
                        original.kind(),
                        original.modifiers(),
                        original.superclass(),
                        original.interfaces(),
                        List.of(nativeMethod),
                        original.fields());
        ExecutableProgram baseline =
                ExecutableProgram.assemble(
                        List.of(
                                new ExecutableClassBinding(
                                        STANDARD,
                                        nativeClass,
                                        ExecutableBodyBindings.of(List.of()))));
        ExecutableMethodBinding replacement =
                new ExecutableMethodBinding(CUSTOM, body(method("run"), CUSTOM));

        ExecutableProgram customized =
                baseline.withMethodReplacement(method("run"), STANDARD, replacement);

        assertSame(replacement.body(), customized.requireMethod(method("run")).body());
        assertThrows(
                MissingExecutableBodyException.class, () -> baseline.requireMethod(method("run")));
        assertFalse(
                customized
                        .declarations()
                        .findMethod(method("run"))
                        .orElseThrow()
                        .modifiers()
                        .contains(JavaStandardModifier.NATIVE));
    }

    @Test
    void aProjectReplacesOneMethodWithoutMutatingTheSharedProgram() {
        ExecutableClassBinding standardClass = classBinding(STANDARD, "run");
        ExecutableProgram baseline = ExecutableProgram.assemble(List.of(standardClass));
        ExecutableMethodKey method = method("run");
        ExecutableMethodBinding replacement =
                new ExecutableMethodBinding(CUSTOM, body(method, CUSTOM));

        ExecutableProgram customized =
                baseline.withMethodReplacement(method, STANDARD, replacement);

        assertSame(replacement.body(), customized.requireMethod(method).body());
        assertEquals(STANDARD, baseline.requireMethod(method).identity());
        assertEquals(CUSTOM, customized.requireMethod(method).identity());
        assertThrows(
                ImplementationBindingException.class,
                () -> customized.withMethodReplacement(method, STANDARD, replacement));
    }

    @Test
    void aClassReplacementSelectsTheWholeDefinitionWithoutMergingOldMethods() {
        ExecutableProgram baseline =
                ExecutableProgram.assemble(List.of(classBinding(STANDARD, "oldMethod")));
        ExecutableClassBinding replacement = classBinding(CUSTOM, "newMethod");

        ExecutableProgram customized = baseline.withClassReplacement(TYPE, STANDARD, replacement);

        assertSame(replacement, customized.requireClass(TYPE));
        assertEquals(CUSTOM, customized.requireMethod(method("newMethod")).identity());
        assertThrows(
                MissingExecutableBodyException.class,
                () -> customized.requireMethod(method("oldMethod")));
        assertEquals(STANDARD, baseline.requireMethod(method("oldMethod")).identity());
    }

    private static ExecutableClassBinding classBinding(
            ImplementationIdentity identity, String methodName) {
        ExecutableMethodKey key = method(methodName);
        ProgramInput input =
                new ProgramInput(ProgramInputKind.JAVA_SOURCE, identity.name(), identity.version());
        ProgramClassDeclaration declaration =
                new ProgramClassDeclaration(
                        TYPE,
                        new ClassDefinitionOrigin(input, origin(identity.name())),
                        ProgramClassKind.CLASS,
                        Set.of(),
                        Optional.empty(),
                        List.of(),
                        List.of(new ProgramMethodDeclaration(key, Set.of(), Optional.empty())),
                        List.of());
        ExecutableBodyBindings methods =
                ExecutableBodyBindings.of(
                        List.of(new ExecutableMethodBinding(identity, body(key, identity))));
        return new ExecutableClassBinding(identity, declaration, methods);
    }

    private static StructuredMethodBody body(
            ExecutableMethodKey method, ImplementationIdentity identity) {
        ExecutableBlockNode root =
                new ExecutableBlockNode(
                        new ExecutableNodeMetadata(
                                new ExecutableNodeId(method, identity.name(), 0),
                                List.of(origin(identity.name()))));
        root.freeze();
        return new StructuredMethodBody(method, root);
    }

    private static ProgramOrigin origin(String source) {
        return new ProgramOrigin(source, InputUnit.UTF16_CODE_UNIT, 0, 1);
    }

    private static ExecutableMethodKey method(String name) {
        return new ExecutableMethodKey(
                TYPE, name, new JavaMethodDescriptor(List.of(), JavaVoidType.VOID));
    }
}
