package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public abstract class JavaVerificationTypeNode extends JavaBytecodeNode {
    private final JvmVerificationTypeTag tag;

    protected JavaVerificationTypeNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JvmVerificationTypeTag tag) {
        super(schema, source, span);
        this.tag = tag;
    }

    public final JvmVerificationTypeTag tag() {
        return tag;
    }
}
