package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaLocalVariableTargetEntryNode extends JavaBytecodeNode {
    public static final NodeKind<JavaLocalVariableTargetEntryNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.LOCAL_VARIABLE_TARGET_ENTRY_NODE,
                    JavaLocalVariableTargetEntryNode.class);

    public static final NodeSchema<JavaLocalVariableTargetEntryNode> SCHEMA = NodeSchema.of(KIND);
    private final int startPc;
    private final int length;
    private final int slot;

    public JavaLocalVariableTargetEntryNode(
            BytesSource source, BytecodeSpan span, int startPc, int length, int slot) {
        super(SCHEMA, source, span);
        this.startPc = startPc;
        this.length = length;
        this.slot = slot;
    }

    public int startPc() {
        return startPc;
    }

    public int length() {
        return length;
    }

    public int slot() {
        return slot;
    }
}
