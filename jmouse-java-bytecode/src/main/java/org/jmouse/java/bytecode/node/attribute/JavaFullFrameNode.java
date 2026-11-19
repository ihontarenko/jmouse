package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaFullFrameNode extends JavaStackMapFrameNode {
    public static final NodeKind<JavaFullFrameNode> KIND =
            new NodeKind<>(JvmNodeNames.FULL_FRAME_NODE, JavaFullFrameNode.class);
    public static final NodeSchema<JavaFullFrameNode> SCHEMA = NodeSchema.of(KIND, LOCALS, STACK);

    public JavaFullFrameNode(
            BytesSource source,
            BytecodeSpan span,
            int frameType,
            int offsetDelta,
            List<JavaVerificationTypeNode> locals,
            List<JavaVerificationTypeNode> stack) {
        super(SCHEMA, source, span, frameType, offsetDelta, 0, locals, stack);
    }
}
