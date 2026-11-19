package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaModulePackagesAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaModulePackagesAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.MODULE_PACKAGES_ATTRIBUTE_NODE,
                    JavaModulePackagesAttributeNode.class);

    public static final NodeSchema<JavaModulePackagesAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final List<Integer> packageIndices;

    public JavaModulePackagesAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<Integer> packageIndices) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.packageIndices = List.copyOf(packageIndices);
    }

    public List<Integer> packageIndices() {
        return packageIndices;
    }
}
