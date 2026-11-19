package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;

public abstract class JavaAttributeNode extends JavaBytecodeNode {
    private final int nameIndex;
    private final JavaConstantPoolNode pool;

    protected JavaAttributeNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex) {
        super(schema, source, span);
        this.pool = pool;
        this.nameIndex = nameIndex;
    }

    public final int nameIndex() {
        return nameIndex;
    }

    public final String name() {
        return pool.utf8(nameIndex);
    }
}
