package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaSameLocalsOneStackItemFrameNode extends JavaStackMapFrameNode {
    public static final NodeKind<JavaSameLocalsOneStackItemFrameNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.SAME_LOCALS_ONE_STACK_ITEM_FRAME_NODE,
                    JavaSameLocalsOneStackItemFrameNode.class);
    public static final NodeSchema<JavaSameLocalsOneStackItemFrameNode> SCHEMA =
            NodeSchema.of(KIND, LOCALS, STACK);

    public JavaSameLocalsOneStackItemFrameNode(
            BytesSource source,
            BytecodeSpan span,
            int frameType,
            int offsetDelta,
            JavaVerificationTypeNode value) {
        super(SCHEMA, source, span, frameType, offsetDelta, 0, List.of(), List.of(value));
    }
}
