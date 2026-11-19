package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaModuleProvideNode extends JavaBytecodeNode {
    public static final NodeKind<JavaModuleProvideNode> KIND =
            new NodeKind<>(JvmNodeNames.MODULE_PROVIDE_NODE, JavaModuleProvideNode.class);

    public static final NodeSchema<JavaModuleProvideNode> SCHEMA = NodeSchema.of(KIND);
    private final int serviceIndex;
    private final List<Integer> implementationIndices;

    public JavaModuleProvideNode(
            BytesSource source,
            BytecodeSpan span,
            int serviceIndex,
            List<Integer> implementationIndices) {
        super(SCHEMA, source, span);
        this.serviceIndex = serviceIndex;
        this.implementationIndices = List.copyOf(implementationIndices);
    }

    public int serviceIndex() {
        return serviceIndex;
    }

    public List<Integer> implementationIndices() {
        return implementationIndices;
    }
}
