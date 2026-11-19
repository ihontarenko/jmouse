package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaModuleRequireNode extends JavaBytecodeNode {
    public static final NodeKind<JavaModuleRequireNode> KIND =
            new NodeKind<>(JvmNodeNames.MODULE_REQUIRE_NODE, JavaModuleRequireNode.class);

    public static final NodeSchema<JavaModuleRequireNode> SCHEMA = NodeSchema.of(KIND);
    private final int moduleIndex;
    private final int accessFlags;
    private final int versionIndex;

    public JavaModuleRequireNode(
            BytesSource source,
            BytecodeSpan span,
            int moduleIndex,
            int accessFlags,
            int versionIndex) {
        super(SCHEMA, source, span);
        this.moduleIndex = moduleIndex;
        this.accessFlags = accessFlags;
        this.versionIndex = versionIndex;
    }

    public int moduleIndex() {
        return moduleIndex;
    }

    public int accessFlags() {
        return accessFlags;
    }

    public int versionIndex() {
        return versionIndex;
    }
}
