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

public final class JavaAnnotationNode extends JavaBytecodeNode {
    public static final NodeKind<JavaAnnotationNode> KIND =
            new NodeKind<>(JvmNodeNames.ANNOTATION_NODE, JavaAnnotationNode.class);
    public static final ChildRole<JavaAnnotationElementNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaAnnotationElementNode.class);
    public static final NodeSchema<JavaAnnotationNode> SCHEMA = NodeSchema.of(KIND, ENTRIES);
    private final int typeIndex;

    public JavaAnnotationNode(
            BytesSource source,
            BytecodeSpan span,
            int typeIndex,
            List<JavaAnnotationElementNode> entries) {
        super(SCHEMA, source, span);
        this.typeIndex = typeIndex;
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public int typeIndex() {
        return typeIndex;
    }

    public List<JavaAnnotationElementNode> entries() {
        return children(ENTRIES);
    }
}
