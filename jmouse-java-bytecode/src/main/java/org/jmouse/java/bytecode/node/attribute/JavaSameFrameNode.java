package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaSameFrameNode extends JavaStackMapFrameNode {
    public static final NodeKind<JavaSameFrameNode> KIND =
            new NodeKind<>(JvmNodeNames.SAME_FRAME_NODE, JavaSameFrameNode.class);
    public static final NodeSchema<JavaSameFrameNode> SCHEMA = NodeSchema.of(KIND, LOCALS, STACK);

    public JavaSameFrameNode(
            BytesSource source, BytecodeSpan span, int frameType, int offsetDelta) {
        super(SCHEMA, source, span, frameType, offsetDelta, 0, List.of(), List.of());
    }
}
