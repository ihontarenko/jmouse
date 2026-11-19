package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmStackMapFrameKind;

import java.util.List;

public abstract class JavaStackMapFrameNode extends JavaBytecodeNode {
    public static final ChildRole<JavaVerificationTypeNode> LOCALS =
            ChildRole.list(JavaBytecodeNames.LOCALS, JavaVerificationTypeNode.class);
    public static final ChildRole<JavaVerificationTypeNode> STACK =
            ChildRole.list(JavaBytecodeNames.STACK, JavaVerificationTypeNode.class);
    private final int frameType;
    private final int offsetDelta;
    private final int choppedLocals;

    protected JavaStackMapFrameNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            int frameType,
            int offsetDelta,
            int choppedLocals,
            List<JavaVerificationTypeNode> locals,
            List<JavaVerificationTypeNode> stack) {
        super(schema, source, span);
        this.frameType = frameType;
        this.offsetDelta = offsetDelta;
        this.choppedLocals = choppedLocals;
        locals.forEach(value -> addChild(LOCALS, value));
        stack.forEach(value -> addChild(STACK, value));
    }

    public final int frameType() {
        return frameType;
    }

    public final JvmStackMapFrameKind kind() {
        return JvmStackMapFrameKind.of(frameType);
    }

    public final int offsetDelta() {
        return offsetDelta;
    }

    public final int choppedLocals() {
        return choppedLocals;
    }

    public final List<JavaVerificationTypeNode> locals() {
        return children(LOCALS);
    }

    public final List<JavaVerificationTypeNode> stack() {
        return children(STACK);
    }
}
