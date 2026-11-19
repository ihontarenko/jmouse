package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaAppendFrameNode extends JavaStackMapFrameNode {
    public static final NodeKind<JavaAppendFrameNode> KIND =
            new NodeKind<>(JvmNodeNames.APPEND_FRAME_NODE, JavaAppendFrameNode.class);
    public static final NodeSchema<JavaAppendFrameNode> SCHEMA = NodeSchema.of(KIND, LOCALS, STACK);

    public JavaAppendFrameNode(
            BytesSource source,
            BytecodeSpan span,
            int frameType,
            int offsetDelta,
            List<JavaVerificationTypeNode> locals) {
        super(SCHEMA, source, span, frameType, offsetDelta, 0, locals, List.of());
    }
}
