package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmConstantKind;

import java.util.Objects;

public abstract class JavaConstantNode extends JavaBytecodeNode {
    private final int index;
    private final JvmConstantKind tag;

    protected JavaConstantNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            int index,
            JvmConstantKind tag) {
        super(schema, source, span);
        if (index <= 0) {
            throw new IllegalArgumentException("A constant must have a positive pool index");
        }
        this.index = index;
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    public final int index() {
        return index;
    }

    public final JvmConstantKind tag() {
        return tag;
    }
}
