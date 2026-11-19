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

public final class JavaTypeAnnotationNode extends JavaBytecodeNode {
    public static final NodeKind<JavaTypeAnnotationNode> KIND =
            new NodeKind<>(JvmNodeNames.TYPE_ANNOTATION_NODE, JavaTypeAnnotationNode.class);
    public static final ChildRole<JavaTypeAnnotationTargetNode> TARGET =
            ChildRole.required(JavaBytecodeNames.TARGET, JavaTypeAnnotationTargetNode.class);
    public static final ChildRole<JavaTypePathEntryNode> PATH =
            ChildRole.list(JavaBytecodeNames.PATH, JavaTypePathEntryNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATION =
            ChildRole.required(JavaBytecodeNames.ANNOTATION, JavaAnnotationNode.class);
    public static final NodeSchema<JavaTypeAnnotationNode> SCHEMA =
            NodeSchema.of(KIND, TARGET, PATH, ANNOTATION);

    public JavaTypeAnnotationNode(
            BytesSource source,
            BytecodeSpan span,
            JavaTypeAnnotationTargetNode target,
            List<JavaTypePathEntryNode> path,
            JavaAnnotationNode annotation) {
        super(SCHEMA, source, span);
        setChild(TARGET, target);
        path.forEach(entry -> addChild(PATH, entry));
        setChild(ANNOTATION, annotation);
    }

    public JavaTypeAnnotationTargetNode target() {
        return child(TARGET);
    }

    public List<JavaTypePathEntryNode> path() {
        return children(PATH);
    }

    public JavaAnnotationNode annotation() {
        return child(ANNOTATION);
    }
}
