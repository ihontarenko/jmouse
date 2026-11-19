package org.jmouse.java.bytecode.node;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;

import java.util.List;

public abstract class JavaMemberNode extends JavaBytecodeNode {
    public static final ChildRole<JavaAttributeNode> ATTRIBUTES =
            ChildRole.list(JavaBytecodeNames.ATTRIBUTES, JavaAttributeNode.class);
    private final JavaConstantPoolNode pool;
    private final int accessFlags;
    private final int nameIndex;
    private final int descriptorIndex;

    protected JavaMemberNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int accessFlags,
            int nameIndex,
            int descriptorIndex,
            List<JavaAttributeNode> attributes) {
        super(schema, source, span);
        this.pool = pool;
        this.accessFlags = accessFlags;
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
        attributes.forEach(attribute -> addChild(ATTRIBUTES, attribute));
    }

    public final int accessFlags() {
        return accessFlags;
    }

    public final int nameIndex() {
        return nameIndex;
    }

    public final int descriptorIndex() {
        return descriptorIndex;
    }

    public final String name() {
        return pool.utf8(nameIndex);
    }

    public final String descriptor() {
        return pool.utf8(descriptorIndex);
    }

    public final List<JavaAttributeNode> attributes() {
        return children(ATTRIBUTES);
    }
}
