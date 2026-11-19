package org.jmouse.vm.graph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.el.InputUnit;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaFieldKey;
import org.jmouse.java.model.symbol.JavaModifier;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaVoidType;
import org.jmouse.vm.graph.requirement.ClassInitializationRequirement;
import org.jmouse.vm.graph.requirement.FieldUsageRequirement;
import org.jmouse.vm.graph.requirement.MethodImplementationRequirement;
import org.jmouse.vm.graph.requirement.TypeInstantiationRequirement;
import org.jmouse.vm.graph.requirement.VirtualInvocationRequirement;
import org.jmouse.vm.program.ClassDefinitionOrigin;
import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramClassDeclaration;
import org.jmouse.vm.program.ProgramClassKind;
import org.jmouse.vm.program.ProgramDeclarationSnapshot;
import org.jmouse.vm.program.ProgramFieldDeclaration;
import org.jmouse.vm.program.ProgramHierarchy;
import org.jmouse.vm.program.ProgramInput;
import org.jmouse.vm.program.ProgramInputKind;
import org.jmouse.vm.program.ProgramMethodDeclaration;
import org.jmouse.vm.program.ProgramOrigin;
import org.jmouse.vm.program.RuntimePackageStrategy;
import org.jmouse.vm.program.StandardClassInitialization;
import org.jmouse.vm.program.StandardVirtualDispatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class ProgramDependenciesTest {

    @Test
    void includesOnlyInstantiatedDispatchTargetsAndTheDeclaringClassOfAStaticField() {
        JavaTypeKey entry = type("Entry");
        JavaTypeKey contract = type("Contract");
        JavaTypeKey base = type("Base");
        JavaTypeKey created = type("Created");
        JavaTypeKey unused = type("Unused");
        ExecutableMethodKey main = method(entry, "main");
        ExecutableMethodKey call = method(contract, "call");
        JavaFieldKey field = new JavaFieldKey(base, "counter", JavaPrimitiveType.INT);
        ProgramClassDeclaration baseClass =
                declaration(
                        base,
                        ProgramClassKind.CLASS,
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of(
                                new ProgramFieldDeclaration(
                                        field,
                                        Set.of(JavaStandardModifier.STATIC),
                                        Optional.empty())));
        ProgramHierarchy hierarchy =
                new ProgramHierarchy(
                        ProgramDeclarationSnapshot.assemble(
                                List.of(
                                        declaration(
                                                entry,
                                                ProgramClassKind.CLASS,
                                                Optional.empty(),
                                                List.of(),
                                                List.of(
                                                        declare(
                                                                main,
                                                                Set.of(
                                                                        JavaStandardModifier
                                                                                .STATIC))),
                                                List.of()),
                                        declaration(
                                                contract,
                                                ProgramClassKind.INTERFACE,
                                                Optional.empty(),
                                                List.of(),
                                                List.of(
                                                        declare(
                                                                call,
                                                                Set.of(
                                                                        JavaStandardModifier.PUBLIC,
                                                                        JavaStandardModifier
                                                                                .ABSTRACT))),
                                                List.of()),
                                        baseClass,
                                        declaration(
                                                created,
                                                ProgramClassKind.CLASS,
                                                Optional.of(base),
                                                List.of(contract),
                                                List.of(
                                                        declare(
                                                                method(created, "call"),
                                                                Set.of(
                                                                        JavaStandardModifier
                                                                                .PUBLIC))),
                                                List.of()),
                                        declaration(
                                                unused,
                                                ProgramClassKind.CLASS,
                                                Optional.empty(),
                                                List.of(contract),
                                                List.of(
                                                        declare(
                                                                method(unused, "call"),
                                                                Set.of(
                                                                        JavaStandardModifier
                                                                                .PUBLIC))),
                                                List.of()))));
        MethodDependencyProvider bodies =
                method -> {
                    ProgramRequirement implementation = new MethodImplementationRequirement(method);
                    if (method.equals(main)) {
                        return List.of(
                                new DependencyRule(
                                        Set.of(implementation),
                                        new VirtualInvocationRequirement(call),
                                        reason()),
                                new DependencyRule(
                                        Set.of(implementation),
                                        new TypeInstantiationRequirement(created),
                                        reason()),
                                new DependencyRule(
                                        Set.of(implementation),
                                        new FieldUsageRequirement(field),
                                        reason()));
                    }
                    return List.of();
                };
        ProgramDependencies dependencies =
                new ProgramDependencies(
                        hierarchy,
                        new StandardVirtualDispatch(
                                hierarchy, RuntimePackageStrategy.moduleScoped()),
                        new StandardClassInitialization(hierarchy),
                        ReceiverTypeStrategy.closedWorld(hierarchy),
                        bodies);

        ReachabilityResult result =
                new ReachabilityAnalyzer(List.of(dependencies))
                        .analyze(
                                List.of(
                                        new EntryPoint(
                                                new MethodImplementationRequirement(main),
                                                reason())));

        assertTrue(result.contains(new MethodImplementationRequirement(method(created, "call"))));
        assertFalse(result.contains(new MethodImplementationRequirement(method(unused, "call"))));
        assertTrue(result.contains(new ClassInitializationRequirement(base)));
        assertTrue(result.contains(new ClassInitializationRequirement(created)));
        assertFalse(result.contains(new ClassInitializationRequirement(contract)));
        assertFalse(result.contains(new ClassInitializationRequirement(unused)));
        assertTrue(
                result
                        .explain(new MethodImplementationRequirement(method(created, "call")))
                        .rules()
                        .stream()
                        .anyMatch(
                                rule ->
                                        rule.reason().kind()
                                                        == StandardDependencyKind.VIRTUAL_DISPATCH
                                                && rule.prerequisites()
                                                        .contains(
                                                                new TypeInstantiationRequirement(
                                                                        created))));
    }

    private static ProgramClassDeclaration declaration(
            JavaTypeKey type,
            ProgramClassKind kind,
            Optional<JavaTypeKey> superclass,
            List<JavaTypeKey> interfaces,
            List<ProgramMethodDeclaration> methods,
            List<ProgramFieldDeclaration> fields) {
        ProgramInput input =
                new ProgramInput(ProgramInputKind.JVM_CLASSFILE, type.binaryName(), "fixture");
        ClassDefinitionOrigin origin =
                new ClassDefinitionOrigin(
                        input, new ProgramOrigin(type.binaryName(), InputUnit.BYTE, 0, 10));
        return new ProgramClassDeclaration(
                type, origin, kind, Set.of(), superclass, interfaces, methods, fields);
    }

    private static JavaTypeKey type(String name) {
        return new JavaTypeKey(new JavaModuleKey(""), name);
    }

    private static ExecutableMethodKey method(JavaTypeKey owner, String name) {
        return new ExecutableMethodKey(
                owner, name, new JavaMethodDescriptor(List.of(), JavaVoidType.VOID));
    }

    private static ProgramMethodDeclaration declare(
            ExecutableMethodKey key, Set<JavaModifier> modifiers) {
        return new ProgramMethodDeclaration(key, modifiers, Optional.empty());
    }

    private static DependencyReason reason() {
        return new DependencyReason(
                StandardDependencyKind.CALL, "Fixture body dependency", List.of());
    }
}
