package org.jmouse.vm.executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.el.InputUnit;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaDeclaredType;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaSpecialType;
import org.jmouse.java.model.type.JavaVoidType;
import org.jmouse.vm.executable.node.ExecutableBlockNode;
import org.jmouse.vm.executable.node.ExecutableCatchNode;
import org.jmouse.vm.executable.node.ExecutableIfNode;
import org.jmouse.vm.executable.node.ExecutableLocalReferenceNode;
import org.jmouse.vm.executable.node.ExecutableResourceNode;
import org.jmouse.vm.executable.node.ExecutableTryNode;
import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramOrigin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class StructuredMethodBodyTest {

    private static final ExecutableMethodKey METHOD =
            new ExecutableMethodKey(
                    new JavaTypeKey(new JavaModuleKey(""), "sample.Example"),
                    "run",
                    new JavaMethodDescriptor(
                            List.of(JavaPrimitiveType.BOOLEAN), JavaVoidType.VOID));
    private static final String OPERATION = "fixture.emission";
    private static final String STANDARD_IF = "fixture.standard.if";
    private static final String STANDARD_TRY = "fixture.standard.try";
    private static final String PROJECT_IF = "fixture.project.if";

    @Test
    void resourcesCatchAndFinallyRemainOwnedVisibleAndInDeclarationOrder() {
        JavaTypeKey resourceType = new JavaTypeKey(METHOD.owner().module(), "sample.Resource");
        JavaTypeKey exceptionType = new JavaTypeKey(METHOD.owner().module(), "sample.Failure");
        ExecutableLocal input =
                new ExecutableLocal(
                        new ExecutableLocalId(METHOD, 0),
                        "input",
                        new JavaDeclaredType(resourceType, List.of(), JavaSpecialType.NONE));
        ExecutableLocal first =
                new ExecutableLocal(new ExecutableLocalId(METHOD, 1), "first", input.type());
        ExecutableLocal second =
                new ExecutableLocal(new ExecutableLocalId(METHOD, 2), "second", input.type());
        ExecutableLocal exception =
                new ExecutableLocal(
                        new ExecutableLocalId(METHOD, 3),
                        "failure",
                        new JavaDeclaredType(exceptionType, List.of(), JavaSpecialType.NONE));
        ResourceCloseBinding close =
                new ResourceCloseBinding(
                        new ExecutableMethodKey(
                                resourceType,
                                "close",
                                new JavaMethodDescriptor(List.of(), JavaVoidType.VOID)),
                        ExecutableInvocationKind.VIRTUAL);
        ExecutableTryNode guarded =
                new ExecutableTryNode(metadata(1), new ExecutableBlockNode(metadata(2)));
        guarded.addResource(
                new ExecutableResourceNode(
                        metadata(3),
                        first,
                        new ExecutableLocalReferenceNode(metadata(4), input),
                        close));
        guarded.addResource(
                new ExecutableResourceNode(
                        metadata(5),
                        second,
                        new ExecutableLocalReferenceNode(metadata(6), input),
                        close));
        guarded.addCatch(
                new ExecutableCatchNode(
                        metadata(7),
                        List.of(exceptionType),
                        exception,
                        new ExecutableBlockNode(metadata(8))));
        guarded.setFinallyBody(new ExecutableBlockNode(metadata(9)));
        ExecutableBlockNode root = new ExecutableBlockNode(metadata(0));
        root.addStatement(guarded);
        root.freeze();
        StructuredMethodBody body = new StructuredMethodBody(METHOD, root);
        List<Node> visited = new ArrayList<>();
        body.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                visited.add(node);
                            }
                        });

        assertEquals(
                List.of("first", "second"),
                guarded.resources().stream().map(resource -> resource.local().name()).toList());
        assertEquals(List.of(exceptionType), guarded.catches().getFirst().alternatives());
        assertEquals(10, visited.stream().distinct().count());
        assertEquals(guarded, guarded.resources().getFirst().getParent());
    }

    @Test
    void structuredBodiesRejectForeignAndRepeatedNodeIdentities() {
        ExecutableBlockNode root = new ExecutableBlockNode(metadata(0));
        root.addStatement(new ExecutableBlockNode(metadata(0)));
        root.freeze();
        assertThrows(IllegalArgumentException.class, () -> new StructuredMethodBody(METHOD, root));

        ExecutableMethodKey other =
                new ExecutableMethodKey(METHOD.owner(), "other", METHOD.descriptor());
        ExecutableBlockNode foreign = new ExecutableBlockNode(metadata(0));
        foreign.freeze();
        assertThrows(
                IllegalArgumentException.class, () -> new StructuredMethodBody(other, foreign));
    }

    @Test
    void unconvertedConditionsAndIncompleteTryRegionsCannotBecomeFrozenBodies() {
        ExecutableLocal number =
                new ExecutableLocal(
                        new ExecutableLocalId(METHOD, 0), "number", JavaPrimitiveType.INT);
        ExecutableIfNode conditional =
                new ExecutableIfNode(
                        metadata(0),
                        new ExecutableLocalReferenceNode(metadata(1), number),
                        new ExecutableBlockNode(metadata(2)));
        assertThrows(IllegalStateException.class, conditional::freeze);
        ExecutableTryNode guarded =
                new ExecutableTryNode(metadata(0), new ExecutableBlockNode(metadata(1)));
        assertThrows(IllegalStateException.class, guarded::freeze);
    }

    @Test
    void oneProjectCanReplaceIfHandlingWithoutChangingAnotherProjectOrTryHandling() {
        ExecutableLocal condition =
                new ExecutableLocal(
                        new ExecutableLocalId(METHOD, 0), "condition", JavaPrimitiveType.BOOLEAN);
        ExecutableIfNode conditional =
                new ExecutableIfNode(
                        metadata(1),
                        new ExecutableLocalReferenceNode(metadata(2), condition),
                        new ExecutableBlockNode(metadata(3)));
        ExecutableTryNode guarded =
                new ExecutableTryNode(metadata(4), new ExecutableBlockNode(metadata(5)));
        guarded.setFinallyBody(new ExecutableBlockNode(metadata(6)));
        ExecutableBlockNode root = new ExecutableBlockNode(metadata(0));
        root.addStatement(conditional);
        root.addStatement(guarded);
        root.freeze();
        StructuredMethodBody body = new StructuredMethodBody(METHOD, root);

        NodeOperationRegistry<Void, String> registrations = new NodeOperationRegistry<>(OPERATION);
        registrations.bind(ExecutableIfNode.KIND, STANDARD_IF, (node, context) -> "standard if");
        registrations.bind(ExecutableTryNode.KIND, STANDARD_TRY, (node, context) -> "standard try");
        NodeOperationRegistry<Void, String> standardProject = registrations.snapshot();
        NodeOperationRegistry<Void, String> customized = standardProject.fork();
        customized.replace(
                ExecutableIfNode.KIND, STANDARD_IF, PROJECT_IF, (node, context) -> "custom if");
        NodeOperationRegistry<Void, String> customProject = customized.snapshot();

        assertEquals(METHOD, body.owner());
        assertEquals("custom if", customProject.apply(conditional, null));
        assertEquals("standard if", standardProject.apply(conditional, null));
        assertEquals("standard try", customProject.apply(guarded, null));
        assertEquals("standard try", standardProject.apply(guarded, null));
        assertEquals(PROJECT_IF, customProject.explain(ExecutableIfNode.KIND).implementation());
    }

    private static ExecutableNodeMetadata metadata(long ordinal) {
        return new ExecutableNodeMetadata(
                new ExecutableNodeId(METHOD, "fixture.source", ordinal),
                List.of(
                        new ProgramOrigin(
                                "Example.java", InputUnit.UTF16_CODE_UNIT, ordinal, ordinal + 1)));
    }
}
