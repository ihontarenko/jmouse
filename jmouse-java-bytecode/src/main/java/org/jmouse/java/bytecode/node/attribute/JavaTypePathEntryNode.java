package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypePathKind;

public final class JavaTypePathEntryNode extends JavaBytecodeNode {
    public static final NodeKind<JavaTypePathEntryNode> KIND =
            new NodeKind<>(JvmNodeNames.TYPE_PATH_ENTRY_NODE, JavaTypePathEntryNode.class);

    public static final NodeSchema<JavaTypePathEntryNode> SCHEMA = NodeSchema.of(KIND);
    private final JvmTypePathKind kind;
    private final int argumentIndex;

    public JavaTypePathEntryNode(
            BytesSource source, BytecodeSpan span, JvmTypePathKind kind, int argumentIndex) {
        super(SCHEMA, source, span);
        this.kind = kind;
        this.argumentIndex = argumentIndex;
    }

    public JvmTypePathKind kind() {
        return kind;
    }

    public int argumentIndex() {
        return argumentIndex;
    }
}
