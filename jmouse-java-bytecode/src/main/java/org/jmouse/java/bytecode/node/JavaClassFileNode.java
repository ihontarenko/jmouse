package org.jmouse.java.bytecode.node;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.model.version.ClassFileVersion;

import java.util.List;

public final class JavaClassFileNode extends JavaBytecodeNode {
    public static final NodeKind<JavaClassFileNode> KIND =
            new NodeKind<>(JvmNodeNames.CLASS_FILE_NODE, JavaClassFileNode.class);
    public static final ChildRole<JavaConstantPoolNode> CONSTANT_POOL =
            ChildRole.required(JavaBytecodeNames.CONSTANT_POOL, JavaConstantPoolNode.class);
    public static final ChildRole<JavaFieldNode> FIELDS =
            ChildRole.list(JavaBytecodeNames.FIELDS, JavaFieldNode.class);
    public static final ChildRole<JavaMethodNode> METHODS =
            ChildRole.list(JavaBytecodeNames.METHODS, JavaMethodNode.class);
    public static final ChildRole<JavaAttributeNode> ATTRIBUTES =
            ChildRole.list(JavaBytecodeNames.ATTRIBUTES, JavaAttributeNode.class);
    public static final NodeSchema<JavaClassFileNode> SCHEMA =
            NodeSchema.of(KIND, CONSTANT_POOL, FIELDS, METHODS, ATTRIBUTES);
    private final ClassFileVersion version;
    private final int accessFlags;
    private final int thisClassIndex;
    private final int superclassIndex;
    private final List<Integer> interfaceIndices;

    public JavaClassFileNode(
            BytesSource source,
            BytecodeSpan span,
            ClassFileVersion version,
            JavaConstantPoolNode pool,
            int accessFlags,
            int thisClassIndex,
            int superclassIndex,
            List<Integer> interfaceIndices,
            List<JavaFieldNode> fields,
            List<JavaMethodNode> methods,
            List<JavaAttributeNode> attributes) {
        super(SCHEMA, source, span);
        this.version = version;
        this.accessFlags = accessFlags;
        this.thisClassIndex = thisClassIndex;
        this.superclassIndex = superclassIndex;
        this.interfaceIndices = List.copyOf(interfaceIndices);
        setChild(CONSTANT_POOL, pool);
        fields.forEach(field -> addChild(FIELDS, field));
        methods.forEach(method -> addChild(METHODS, method));
        attributes.forEach(attribute -> addChild(ATTRIBUTES, attribute));
    }

    public ClassFileVersion version() {
        return version;
    }

    public int accessFlags() {
        return accessFlags;
    }

    public int thisClassIndex() {
        return thisClassIndex;
    }

    public int superclassIndex() {
        return superclassIndex;
    }

    public List<Integer> interfaceIndices() {
        return interfaceIndices;
    }

    public String internalName() {
        return pool().className(thisClassIndex);
    }

    public JavaConstantPoolNode pool() {
        return child(CONSTANT_POOL);
    }

    public List<JavaFieldNode> fields() {
        return children(FIELDS);
    }

    public List<JavaMethodNode> methods() {
        return children(METHODS);
    }

    public List<JavaAttributeNode> attributes() {
        return children(ATTRIBUTES);
    }
}
