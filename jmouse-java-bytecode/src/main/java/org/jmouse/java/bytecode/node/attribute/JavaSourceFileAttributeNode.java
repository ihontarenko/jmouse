package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaSourceFileAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaSourceFileAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.SOURCE_FILE_ATTRIBUTE_NODE, JavaSourceFileAttributeNode.class);

    public static final NodeSchema<JavaSourceFileAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final int sourceFileIndex;

    public JavaSourceFileAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int sourceFileIndex) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.sourceFileIndex = sourceFileIndex;
    }

    public int sourceFileIndex() {
        return sourceFileIndex;
    }
}
