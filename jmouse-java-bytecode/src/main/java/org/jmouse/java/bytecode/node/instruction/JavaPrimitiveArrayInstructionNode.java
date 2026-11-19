package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;
import org.jmouse.java.bytecode.vocabulary.JvmPrimitiveArrayType;

public final class JavaPrimitiveArrayInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaPrimitiveArrayInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.PRIMITIVE_ARRAY_INSTRUCTION_NODE,
                    JavaPrimitiveArrayInstructionNode.class);

    public static final NodeSchema<JavaPrimitiveArrayInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final JvmPrimitiveArrayType elementType;

    public JavaPrimitiveArrayInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            JvmPrimitiveArrayType elementType) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.elementType = elementType;
    }

    public JvmPrimitiveArrayType elementType() {
        return elementType;
    }
}
