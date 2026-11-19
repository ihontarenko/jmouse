package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaParameterAnnotationsNode extends JavaBytecodeNode {
    public static final NodeKind<JavaParameterAnnotationsNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.PARAMETER_ANNOTATIONS_NODE, JavaParameterAnnotationsNode.class);
    public static final ChildRole<JavaAnnotationNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaAnnotationNode.class);
    public static final NodeSchema<JavaParameterAnnotationsNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);
    private final int parameter;

    public JavaParameterAnnotationsNode(
            BytesSource source,
            BytecodeSpan span,
            int parameter,
            List<JavaAnnotationNode> entries) {
        super(SCHEMA, source, span);
        this.parameter = parameter;
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public int parameter() {
        return parameter;
    }

    public List<JavaAnnotationNode> entries() {
        return children(ENTRIES);
    }
}
