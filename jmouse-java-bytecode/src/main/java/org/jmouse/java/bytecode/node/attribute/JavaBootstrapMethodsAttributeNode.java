package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaBootstrapMethodsAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaBootstrapMethodsAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.BOOTSTRAP_METHODS_ATTRIBUTE_NODE,
                    JavaBootstrapMethodsAttributeNode.class);
    public static final ChildRole<JavaBootstrapMethodNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaBootstrapMethodNode.class);
    public static final NodeSchema<JavaBootstrapMethodsAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaBootstrapMethodsAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaBootstrapMethodNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaBootstrapMethodNode> entries() {
        return children(ENTRIES);
    }
}
