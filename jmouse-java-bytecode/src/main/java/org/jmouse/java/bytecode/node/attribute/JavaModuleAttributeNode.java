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

public final class JavaModuleAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaModuleAttributeNode> KIND =
            new NodeKind<>(JvmNodeNames.MODULE_ATTRIBUTE_NODE, JavaModuleAttributeNode.class);
    public static final ChildRole<JavaModuleRequireNode> REQUIRES =
            ChildRole.list(JavaBytecodeNames.REQUIRES, JavaModuleRequireNode.class);
    public static final ChildRole<JavaModuleExportNode> EXPORTS =
            ChildRole.list(JavaBytecodeNames.EXPORTS, JavaModuleExportNode.class);
    public static final ChildRole<JavaModuleOpenNode> OPENS =
            ChildRole.list(JavaBytecodeNames.OPENS, JavaModuleOpenNode.class);
    public static final ChildRole<JavaModuleProvideNode> PROVIDES =
            ChildRole.list(JavaBytecodeNames.PROVIDES, JavaModuleProvideNode.class);
    public static final NodeSchema<JavaModuleAttributeNode> SCHEMA =
            NodeSchema.of(KIND, REQUIRES, EXPORTS, OPENS, PROVIDES);
    private final int moduleNameIndex;
    private final int moduleFlags;
    private final int moduleVersionIndex;
    private final List<Integer> usesIndices;

    public JavaModuleAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int moduleNameIndex,
            int moduleFlags,
            int moduleVersionIndex,
            List<Integer> usesIndices,
            List<JavaModuleRequireNode> requires,
            List<JavaModuleExportNode> exports,
            List<JavaModuleOpenNode> opens,
            List<JavaModuleProvideNode> provides) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.moduleNameIndex = moduleNameIndex;
        this.moduleFlags = moduleFlags;
        this.moduleVersionIndex = moduleVersionIndex;
        this.usesIndices = List.copyOf(usesIndices);
        requires.forEach(value -> addChild(REQUIRES, value));
        exports.forEach(value -> addChild(EXPORTS, value));
        opens.forEach(value -> addChild(OPENS, value));
        provides.forEach(value -> addChild(PROVIDES, value));
    }

    public int moduleNameIndex() {
        return moduleNameIndex;
    }

    public int moduleFlags() {
        return moduleFlags;
    }

    public int moduleVersionIndex() {
        return moduleVersionIndex;
    }

    public List<Integer> usesIndices() {
        return usesIndices;
    }

    public List<JavaModuleRequireNode> requires() {
        return children(REQUIRES);
    }

    public List<JavaModuleExportNode> exports() {
        return children(EXPORTS);
    }

    public List<JavaModuleOpenNode> opens() {
        return children(OPENS);
    }

    public List<JavaModuleProvideNode> provides() {
        return children(PROVIDES);
    }
}
