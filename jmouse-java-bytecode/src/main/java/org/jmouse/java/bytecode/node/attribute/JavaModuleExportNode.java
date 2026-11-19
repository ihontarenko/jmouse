package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaModuleExportNode extends JavaBytecodeNode {
    public static final NodeKind<JavaModuleExportNode> KIND =
            new NodeKind<>(JvmNodeNames.MODULE_EXPORT_NODE, JavaModuleExportNode.class);

    public static final NodeSchema<JavaModuleExportNode> SCHEMA = NodeSchema.of(KIND);
    private final int packageIndex;
    private final int accessFlags;
    private final List<Integer> targetModuleIndices;

    public JavaModuleExportNode(
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
