package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaModuleMainClassAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaModuleMainClassAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.MODULE_MAIN_CLASS_ATTRIBUTE_NODE,
                    JavaModuleMainClassAttributeNode.class);

    public static final NodeSchema<JavaModuleMainClassAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final int mainClassIndex;

    public JavaModuleMainClassAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int mainClassIndex) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.mainClassIndex = mainClassIndex;
    }

    public int mainClassIndex() {
        return mainClassIndex;
    }
}
