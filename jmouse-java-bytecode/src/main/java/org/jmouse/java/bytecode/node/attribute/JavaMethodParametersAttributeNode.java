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

public final class JavaMethodParametersAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaMethodParametersAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.METHOD_PARAMETERS_ATTRIBUTE_NODE,
                    JavaMethodParametersAttributeNode.class);
    public static final ChildRole<JavaMethodParameterNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaMethodParameterNode.class);
    public static final NodeSchema<JavaMethodParametersAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaMethodParametersAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaMethodParameterNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaMethodParameterNode> entries() {
        return children(ENTRIES);
    }
}
