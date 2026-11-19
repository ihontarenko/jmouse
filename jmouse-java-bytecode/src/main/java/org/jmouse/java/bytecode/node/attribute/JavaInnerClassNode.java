package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaInnerClassNode extends JavaBytecodeNode {
    public static final NodeKind<JavaInnerClassNode> KIND =
            new NodeKind<>(JvmNodeNames.INNER_CLASS_NODE, JavaInnerClassNode.class);

    public static final NodeSchema<JavaInnerClassNode> SCHEMA = NodeSchema.of(KIND);
    private final int innerClassIndex;
    private final int outerClassIndex;
    private final int innerNameIndex;
    private final int accessFlags;

    public JavaInnerClassNode(
            BytesSource source,
            BytecodeSpan span,
            int innerClassIndex,
            int outerClassIndex,
            int innerNameIndex,
            int accessFlags) {
        super(SCHEMA, source, span);
        this.innerClassIndex = innerClassIndex;
        this.outerClassIndex = outerClassIndex;
        this.innerNameIndex = innerNameIndex;
        this.accessFlags = accessFlags;
    }

    public int innerClassIndex() {
        return innerClassIndex;
    }

    public int outerClassIndex() {
        return outerClassIndex;
    }

    public int innerNameIndex() {
        return innerNameIndex;
    }

    public int accessFlags() {
        return accessFlags;
    }
}
