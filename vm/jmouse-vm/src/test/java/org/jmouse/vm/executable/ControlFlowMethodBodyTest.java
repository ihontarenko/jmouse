package org.jmouse.vm.executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.el.InputUnit;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaVoidType;
import org.jmouse.vm.executable.node.ExecutableBasicBlockNode;
import org.jmouse.vm.executable.node.ExecutableBranchTerminatorNode;
import org.jmouse.vm.executable.node.ExecutableControlFlowNode;
import org.jmouse.vm.executable.node.ExecutableJumpTerminatorNode;
import org.jmouse.vm.executable.node.ExecutableLocalReferenceNode;
import org.jmouse.vm.executable.node.ExecutableReturnTerminatorNode;
import org.jmouse.vm.program.ExecutableMethodKey;
import org.jmouse.vm.program.ProgramOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class ControlFlowMethodBodyTest {

    private static final ExecutableMethodKey METHOD =
            new ExecutableMethodKey(
                    new JavaTypeKey(new JavaModuleKey(""), "sample.Example"),
                    "run",
                    new JavaMethodDescriptor(
                            List.of(JavaPrimitiveType.BOOLEAN), JavaVoidType.VOID));

    @Test
    void normalAndOrderedExceptionalEdgesRemainSeparateInTheCommonBodyContract() {
        BasicBlockId entryId = new BasicBlockId(METHOD, 0);
        BasicBlockId leftId = new BasicBlockId(METHOD, 1);
        BasicBlockId rightId = new BasicBlockId(METHOD, 2);
        ExecutableLocal condition =
                new ExecutableLocal(
                        new ExecutableLocalId(METHOD, 0), "condition", JavaPrimitiveType.BOOLEAN);
        ExecutableBasicBlockNode entry =
                new ExecutableBasicBlockNode(
                        metadata(1),
                        entryId,
                        new ExecutableBranchTerminatorNode(
                                metadata(2),
                                new ExecutableLocalReferenceNode(metadata(3), condition),
                                leftId,
                                rightId));
        JavaTypeKey failure = new JavaTypeKey(METHOD.owner().module(), "sample.Failure");
        ExceptionHandlerEdge specific = new ExceptionHandlerEdge(Optional.of(failure), rightId);
        ExceptionHandlerEdge catchAll = new ExceptionHandlerEdge(Optional.empty(), leftId);
        entry.addExceptionHandler(specific);
        entry.addExceptionHandler(catchAll);
        ExecutableControlFlowNode root = new ExecutableControlFlowNode(metadata(0), entryId);
        root.addBlock(entry);
        root.addBlock(
                new ExecutableBasicBlockNode(
                        metadata(4), leftId, new ExecutableReturnTerminatorNode(metadata(5))));
        root.addBlock(
                new ExecutableBasicBlockNode(
                        metadata(6), rightId, new ExecutableReturnTerminatorNode(metadata(7))));
        root.freeze();

        ExecutableMethodBody body = new ControlFlowMethodBody(METHOD, root);

        assertEquals(METHOD, body.owner());
        assertEquals(
                Set.of(leftId, rightId), ((ControlFlowMethodBody) body).normalSuccessors(entryId));
        assertEquals(
                List.of(specific, catchAll),
                ((ControlFlowMethodBody) body).exceptionHandlers(entryId));
    }

    @Test
    void aMissingBranchTargetCannotBecomeAFrozenControlFlowBody() {
        BasicBlockId entryId = new BasicBlockId(METHOD, 0);
        BasicBlockId missingId = new BasicBlockId(METHOD, 1);
        ExecutableControlFlowNode root = new ExecutableControlFlowNode(metadata(0), entryId);
        root.addBlock(
                new ExecutableBasicBlockNode(
                        metadata(1),
                        entryId,
                        new ExecutableJumpTerminatorNode(metadata(2), missingId)));

        assertThrows(IllegalStateException.class, root::freeze);
    }

    private static ExecutableNodeMetadata metadata(long ordinal) {
        return new ExecutableNodeMetadata(
                new ExecutableNodeId(METHOD, "fixture.binary", ordinal),
                List.of(new ProgramOrigin("Example.class", InputUnit.BYTE, ordinal, ordinal + 1)));
    }
}
