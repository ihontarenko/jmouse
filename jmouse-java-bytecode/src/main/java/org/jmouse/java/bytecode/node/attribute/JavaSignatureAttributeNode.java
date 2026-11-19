package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.signature.JvmTextNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaSignatureAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaSignatureAttributeNode> KIND =
            new NodeKind<>(JvmNodeNames.SIGNATURE_ATTRIBUTE_NODE, JavaSignatureAttributeNode.class);

    public static final ChildRole<JvmTextNode> SYNTAX =
            ChildRole.required(JavaBytecodeNames.SIGNATURE_SYNTAX, JvmTextNode.class);
    public static final NodeSchema<JavaSignatureAttributeNode> SCHEMA = NodeSchema.of(KIND, SYNTAX);
    private final int signatureIndex;

    public JavaSignatureAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int signatureIndex,
            JvmTextNode syntax) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.signatureIndex = signatureIndex;
        setChild(SYNTAX, syntax);
    }

    public int signatureIndex() {
        return signatureIndex;
    }

    public JvmTextNode syntax() {
        return child(SYNTAX);
    }
}
