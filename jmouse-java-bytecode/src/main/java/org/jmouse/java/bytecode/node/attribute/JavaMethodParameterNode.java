package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaMethodParameterNode extends JavaBytecodeNode {
    public static final NodeKind<JavaMethodParameterNode> KIND =
            new NodeKind<>(JvmNodeNames.METHOD_PARAMETER_NODE, JavaMethodParameterNode.class);

    public static final NodeSchema<JavaMethodParameterNode> SCHEMA = NodeSchema.of(KIND);
    private final int nameIndex;
    private final int accessFlags;

    public JavaMethodParameterNode(
            BytesSource source, BytecodeSpan span, int nameIndex, int accessFlags) {
        super(SCHEMA, source, span);
        this.nameIndex = nameIndex;
        this.accessFlags = accessFlags;
    }

    public int nameIndex() {
        return nameIndex;
    }

    public int accessFlags() {
        return accessFlags;
    }
}
