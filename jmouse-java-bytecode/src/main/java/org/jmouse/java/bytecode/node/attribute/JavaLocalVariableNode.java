package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaLocalVariableNode extends JavaBytecodeNode {
    public static final NodeKind<JavaLocalVariableNode> KIND =
            new NodeKind<>(JvmNodeNames.LOCAL_VARIABLE_NODE, JavaLocalVariableNode.class);

    public static final NodeSchema<JavaLocalVariableNode> SCHEMA = NodeSchema.of(KIND);
    private final int startPc;
    private final int length;
    private final int nameIndex;
    private final int typeIndex;
    private final int slot;

    public JavaLocalVariableNode(
            BytesSource source,
            BytecodeSpan span,
            int startPc,
            int length,
            int nameIndex,
            int typeIndex,
            int slot) {
        super(SCHEMA, source, span);
        this.startPc = startPc;
        this.length = length;
        this.nameIndex = nameIndex;
        this.typeIndex = typeIndex;
        this.slot = slot;
    }

    public int startPc() {
        return startPc;
    }

    public int length() {
        return length;
    }

    public int nameIndex() {
        return nameIndex;
    }

    public int typeIndex() {
        return typeIndex;
    }

    public int slot() {
        return slot;
    }
}
