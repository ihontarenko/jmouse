package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

import java.util.List;

public final class JavaLocalVariableTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaLocalVariableTargetNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.LOCAL_VARIABLE_TARGET_NODE, JavaLocalVariableTargetNode.class);
    public static final ChildRole<JavaLocalVariableTargetEntryNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaLocalVariableTargetEntryNode.class);
    public static final NodeSchema<JavaLocalVariableTargetNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaLocalVariableTargetNode(
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType,
            List<JavaLocalVariableTargetEntryNode> entries) {
        super(SCHEMA, source, span, targetType);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaLocalVariableTargetEntryNode> entries() {
        return children(ENTRIES);
    }
}
