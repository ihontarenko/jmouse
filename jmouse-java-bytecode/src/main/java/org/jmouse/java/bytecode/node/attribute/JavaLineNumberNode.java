package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaLineNumberNode extends JavaBytecodeNode {
    public static final NodeKind<JavaLineNumberNode> KIND =
            new NodeKind<>(JvmNodeNames.LINE_NUMBER_NODE, JavaLineNumberNode.class);

    public static final NodeSchema<JavaLineNumberNode> SCHEMA = NodeSchema.of(KIND);
    private final int startPc;
    private final int lineNumber;

    public JavaLineNumberNode(BytesSource source, BytecodeSpan span, int startPc, int lineNumber) {
        super(SCHEMA, source, span);
        this.startPc = startPc;
        this.lineNumber = lineNumber;
    }

    public int startPc() {
        return startPc;
    }

    public int lineNumber() {
        return lineNumber;
    }
}
