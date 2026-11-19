package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaModuleOpenNode extends JavaBytecodeNode {
    public static final NodeKind<JavaModuleOpenNode> KIND =
            new NodeKind<>(JvmNodeNames.MODULE_OPEN_NODE, JavaModuleOpenNode.class);

    public static final NodeSchema<JavaModuleOpenNode> SCHEMA = NodeSchema.of(KIND);
    private final int packageIndex;
    private final int accessFlags;
    private final List<Integer> targetModuleIndices;

    public JavaModuleOpenNode(
            BytesSource source,
            BytecodeSpan span,
            int packageIndex,
            int accessFlags,
            List<Integer> targetModuleIndices) {
        super(SCHEMA, source, span);
        this.packageIndex = packageIndex;
        this.accessFlags = accessFlags;
        this.targetModuleIndices = List.copyOf(targetModuleIndices);
    }

    public int packageIndex() {
        return packageIndex;
    }

    public int accessFlags() {
        return accessFlags;
    }

    public List<Integer> targetModuleIndices() {
        return targetModuleIndices;
    }
}
