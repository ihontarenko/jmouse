package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaChopFrameNode extends JavaStackMapFrameNode {
    public static final NodeKind<JavaChopFrameNode> KIND =
            new NodeKind<>(JvmNodeNames.CHOP_FRAME_NODE, JavaChopFrameNode.class);
    public static final NodeSchema<JavaChopFrameNode> SCHEMA = NodeSchema.of(KIND, LOCALS, STACK);

    public JavaChopFrameNode(
            BytesSource source,
            BytecodeSpan span,
            int frameType,
            int offsetDelta,
            int choppedLocals) {
        super(SCHEMA, source, span, frameType, offsetDelta, choppedLocals, List.of(), List.of());
    }
}
