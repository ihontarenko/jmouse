package org.jmouse.vm.executable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jmouse.el.InputUnit;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.vm.executable.dependency.ExecutableDependencyContext;
import org.jmouse.vm.executable.dependency.ExecutableDependencyLimits;
import org.jmouse.vm.executable.dependency.ExecutableDependencyOperations;
import org.jmouse.vm.executable.dependency.ExecutableDependencyLimitException;
import org.jmouse.vm.executable.dependency.ExecutableDependencyLimit;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaDeclaredType;
import org.jmouse.java.model.type.JavaSpecialType;
import org.jmouse.java.model.type.JavaVoidType;
import org.jmouse.vm.executable.node.ExecutableBasicBlockNode;
import org.jmouse.vm.executable.node.ExecutableBlockNode;
import org.jmouse.vm.executable.node.ExecutableCallNode;
import org.jmouse.vm.executable.node.ExecutableCatchNode;
import org.jmouse.vm.executable.node.ExecutableControlFlowNode;
import org.jmouse.vm.executable.node.ExecutableEvaluationNode;
import org.jmouse.vm.executable.node.ExecutableJumpTerminatorNode;
import org.jmouse.vm.executable.node.ExecutableLocalReferenceNode;
import org.jmouse.vm.executable.node.ExecutableResourceNode;
import org.jmouse.vm.executable.node.ExecutableReturnTerminatorNode;
import org.jmouse.vm.executable.node.ExecutableTryNode;
import org.jmouse.vm.graph.DependencyReason;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.EntryPoint;
import org.jmouse.vm.graph.ProgramDependencies;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.ReachabilityAnalyzer;
import org.jmouse.vm.graph.ReachabilityResult;
import org.jmouse.vm.graph.ReceiverTypeStrategy;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.MethodImplementationRequirement;
import org.jmouse.vm.graph.requirement.RuntimeComponentRequirement;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;
import org.jmouse.vm.graph.requirement.VirtualInvocationRequirement;
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
import org.jmouse.vm.program.StandardClassInitialization;
import org.jmouse.vm.program.StandardVirtualDispatch;
import org.jmouse.vm.runtime.JavaRuntimeComponents;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

class ExecutableMethodDependenciesTest {

    private static final JavaTypeKey TYPE =
            new JavaTypeKey(new JavaModuleKey(""), "sample.Example");
    private static final ImplementationIdentity STANDARD =
            new ImplementationIdentity("fixture.standard", "1");
    private static final ImplementationIdentity CUSTOM =
            new ImplementationIdentity("fixture.custom", "1");

    @Test
    void anUnboundedOperationIsLimitedInsideAnIsolatedConfigurationSnapshot() {
        ExecutableProgram program = program();
        NodeOperationRegistry<ExecutableDependencyContext, Iterable<DependencyRule>> baseline =
                ExecutableDependencyOperations.standard();
        NodeOperationRegistry<ExecutableDependencyContext, Iterable<DependencyRule>> customized = baseline.fork();
        String previous = customized.explain(ExecutableBlockNode.KIND).implementation();
        String custom = "fixture.unbounded";
        customized.replace(ExecutableBlockNode.KIND, previous, custom, (node, context) ->
                () -> Stream.generate(() -> context.rule(node,
                        new MethodImplementationRequirement(method("left")), StandardDependencyKind.CALL)).iterator());
        ExecutableMethodDependencies selected = new ExecutableMethodDependencies(program, customized,
                new ExecutableDependencyLimits(100, 3));
        customized.replace(ExecutableBlockNode.KIND, custom, "fixture.changed", (node, context) -> List.of());

        ExecutableDependencyLimitException failure = assertThrows(ExecutableDependencyLimitException.class,
                () -> selected.dependencies(method("main")));

        assertEquals(ExecutableDependencyLimit.CONTRIBUTIONS, failure.limit());
        Set<ProgramRequirement> original = new HashSet<>();
        new ExecutableMethodDependencies(program, baseline, ExecutableDependencyLimits.defaults())
                .dependencies(method("main")).forEach(rule -> original.add(rule.required()));
        assertTrue(original.contains(new MethodImplementationRequirement(method("left"))));
    }

    @Test
    void resourceClosingAndExceptionTypesContributeDependenciesWithTheirRuntimeProtocol() {
        ExecutableMethodKey owner = method("main");
        JavaTypeKey resourceType = new JavaTypeKey(TYPE.module(), "sample.Resource");
        JavaTypeKey failureType = new JavaTypeKey(TYPE.module(), "sample.Failure");
        ExecutableLocal input =
                new ExecutableLocal(
                        new ExecutableLocalId(owner, 0),
                        "input",
                        new JavaDeclaredType(resourceType, List.of(), JavaSpecialType.NONE));
        ExecutableLocal resource =
                new ExecutableLocal(new ExecutableLocalId(owner, 1), "resource", input.type());
        ExecutableLocal failure =
                new ExecutableLocal(
                        new ExecutableLocalId(owner, 2),
                        "failure",
                        new JavaDeclaredType(failureType, List.of(), JavaSpecialType.NONE));
        ExecutableMethodKey close =
                new ExecutableMethodKey(
                        resourceType,
                        "close",
                        new JavaMethodDescriptor(List.of(), JavaVoidType.VOID));
        ExecutableTryNode guarded =
                new ExecutableTryNode(
                        metadata(owner, CUSTOM, 1),
                        new ExecutableBlockNode(metadata(owner, CUSTOM, 2)));
        guarded.addResource(
                new ExecutableResourceNode(
                        metadata(owner, CUSTOM, 3),
                        resource,
                        new ExecutableLocalReferenceNode(metadata(owner, CUSTOM, 4), input),
                        new ResourceCloseBinding(close, ExecutableInvocationKind.VIRTUAL)));
        guarded.addCatch(
                new ExecutableCatchNode(
                        metadata(owner, CUSTOM, 5),
                        List.of(failureType),
                        failure,
                        new ExecutableBlockNode(metadata(owner, CUSTOM, 6))));
        ExecutableBlockNode root = new ExecutableBlockNode(metadata(owner, CUSTOM, 0));
        root.addStatement(guarded);
        root.freeze();
        ExecutableProgram selected =
                program()
                        .withMethodReplacement(
                                owner,
                                STANDARD,
                                new ExecutableMethodBinding(
                                        CUSTOM, new StructuredMethodBody(owner, root)));
        Set<ProgramRequirement> required = new HashSet<>();

        new ExecutableMethodDependencies(selected)
                .dependencies(owner)
                .forEach(rule -> required.add(rule.required()));

        assertTrue(required.contains(new VirtualInvocationRequirement(close)));
        assertTrue(required.contains(new TypeReachabilityRequirement(failureType)));
        assertTrue(
                required.contains(
                        new RuntimeComponentRequirement(JavaRuntimeComponents.RESOURCE_CLOSING)));
        assertFalse(required.contains(new MethodImplementationRequirement(method("left"))));
    }

    @Test
    void unreachableControlFlowBlocksDoNotIntroduceExecutableDependencies() {
        ExecutableMethodKey owner = method("main");
        BasicBlockId entryId = new BasicBlockId(owner, 0);
        BasicBlockId liveId = new BasicBlockId(owner, 1);
        BasicBlockId deadId = new BasicBlockId(owner, 2);
        ExecutableControlFlowNode root =
                new ExecutableControlFlowNode(metadata(owner, CUSTOM, 0), entryId);
        root.addBlock(
                new ExecutableBasicBlockNode(
                        metadata(owner, CUSTOM, 1),
                        entryId,
                        new ExecutableJumpTerminatorNode(metadata(owner, CUSTOM, 2), liveId)));
        ExecutableBasicBlockNode live =
                new ExecutableBasicBlockNode(
                        metadata(owner, CUSTOM, 3),
                        liveId,
                        new ExecutableReturnTerminatorNode(metadata(owner, CUSTOM, 6)));
        live.addOperation(
                new ExecutableEvaluationNode(
                        metadata(owner, CUSTOM, 4),
                        new ExecutableCallNode(
                                metadata(owner, CUSTOM, 5),
                                JavaVoidType.VOID,
                                method("left"),
                                ExecutableInvocationKind.STATIC)));
        root.addBlock(live);
        ExecutableBasicBlockNode dead =
                new ExecutableBasicBlockNode(
                        metadata(owner, CUSTOM, 7),
                        deadId,
                        new ExecutableReturnTerminatorNode(metadata(owner, CUSTOM, 10)));
        dead.addOperation(
                new ExecutableEvaluationNode(
                        metadata(owner, CUSTOM, 8),
                        new ExecutableCallNode(
                                metadata(owner, CUSTOM, 9),
                                JavaVoidType.VOID,
                                method("right"),
                                ExecutableInvocationKind.STATIC)));
        root.addBlock(dead);
        root.freeze();
        ExecutableProgram program =
                program()
                        .withMethodReplacement(
                                owner,
                                STANDARD,
                                new ExecutableMethodBinding(
                                        CUSTOM, new ControlFlowMethodBody(owner, root)));

        ReachabilityResult result = graph(program);

        assertTrue(result.contains(new MethodImplementationRequirement(method("left"))));
        assertFalse(result.contains(new MethodImplementationRequirement(method("right"))));
    }

    @Test
    void methodReplacementChangesActualDependencyClosureInOnlyTheSelectedProject() {
        ExecutableProgram baseline = program();
        ExecutableMethodBinding replacement = binding("main", "right", CUSTOM);
        ExecutableProgram customized =
                baseline.withMethodReplacement(method("main"), STANDARD, replacement);

        ReachabilityResult originalGraph = graph(baseline);
        ReachabilityResult customGraph = graph(customized);

        assertTrue(originalGraph.contains(new MethodImplementationRequirement(method("left"))));
        assertFalse(originalGraph.contains(new MethodImplementationRequirement(method("right"))));
        assertTrue(customGraph.contains(new MethodImplementationRequirement(method("right"))));
        assertFalse(customGraph.contains(new MethodImplementationRequirement(method("left"))));
        assertTrue(
                customGraph
                        .reasonsFor(new MethodImplementationRequirement(method("right")))
                        .stream()
                        .anyMatch(
                                rule ->
                                        rule.reason().origins().stream()
                                                .anyMatch(
                                                        origin ->
                                                                origin.source()
                                                                        .equals(CUSTOM.name()))));
    }

    private static ReachabilityResult graph(ExecutableProgram program) {
        ProgramHierarchy hierarchy = new ProgramHierarchy(program.declarations());
        ProgramDependencies dependencies =
                new ProgramDependencies(
                        hierarchy,
                        new StandardVirtualDispatch(
                                hierarchy, RuntimePackageStrategy.moduleScoped()),
                        new StandardClassInitialization(hierarchy),
                        ReceiverTypeStrategy.closedWorld(hierarchy),
                        new ExecutableMethodDependencies(program));
        return new ReachabilityAnalyzer(List.of(dependencies))
                .analyze(
                        List.of(
                                new EntryPoint(
                                        new MethodImplementationRequirement(method("main")),
                                        new DependencyReason(
                                                StandardDependencyKind.ENTRY_POINT,
                                                "Fixture entry point",
                                                List.of()))));
    }

    private static ExecutableProgram program() {
        List<ProgramMethodDeclaration> methods =
                List.of("main", "left", "right").stream()
                        .map(
                                name ->
                                        new ProgramMethodDeclaration(
                                                method(name),
                                                Set.of(JavaStandardModifier.STATIC),
                                                Optional.empty()))
                        .toList();
        ProgramInput input =
                new ProgramInput(ProgramInputKind.JAVA_SOURCE, "Example.java", "fixture");
        ProgramClassDeclaration declaration =
                new ProgramClassDeclaration(
                        TYPE,
                        new ClassDefinitionOrigin(input, origin("Example.java")),
                        ProgramClassKind.CLASS,
                        Set.of(),
                        Optional.empty(),
                        List.of(),
                        methods,
                        List.of());
        ExecutableBodyBindings bindings =
                ExecutableBodyBindings.of(
                        List.of(
                                binding("main", "left", STANDARD),
                                binding("left", null, STANDARD),
                                binding("right", null, STANDARD)));
        return ExecutableProgram.assemble(
                List.of(new ExecutableClassBinding(STANDARD, declaration, bindings)));
    }

    private static ExecutableMethodBinding binding(
            String name, String called, ImplementationIdentity identity) {
        ExecutableMethodKey owner = method(name);
        ExecutableBlockNode root = new ExecutableBlockNode(metadata(owner, identity, 0));
        if (called != null) {
            ExecutableCallNode call =
                    new ExecutableCallNode(
                            metadata(owner, identity, 2),
                            JavaVoidType.VOID,
                            method(called),
                            ExecutableInvocationKind.STATIC);
            root.addStatement(new ExecutableEvaluationNode(metadata(owner, identity, 1), call));
        }
        root.freeze();
        return new ExecutableMethodBinding(identity, new StructuredMethodBody(owner, root));
    }

    private static ExecutableNodeMetadata metadata(
            ExecutableMethodKey method, ImplementationIdentity identity, long ordinal) {
        return new ExecutableNodeMetadata(
                new ExecutableNodeId(method, identity.name(), ordinal),
                List.of(origin(identity.name())));
    }

    private static ProgramOrigin origin(String source) {
        return new ProgramOrigin(source, InputUnit.UTF16_CODE_UNIT, 0, 1);
    }

    private static ExecutableMethodKey method(String name) {
        return new ExecutableMethodKey(
                TYPE, name, new JavaMethodDescriptor(List.of(), JavaVoidType.VOID));
    }
}
