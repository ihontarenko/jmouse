package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

import java.util.List;
import java.util.stream.Collectors;

public final class JvmClassTypeSignatureNode extends JvmReferenceTypeSignatureNode {
    public static final NodeKind<JvmClassTypeSignatureNode> KIND =
            new NodeKind<>(JvmTextNames.CLASS_TYPE_SIGNATURE, JvmClassTypeSignatureNode.class);
    public static final ChildRole<JvmClassTypeSegmentNode> SEGMENTS =
            ChildRole.list(JvmTextNames.SEGMENTS, JvmClassTypeSegmentNode.class);
    public static final NodeSchema<JvmClassTypeSignatureNode> SCHEMA =
            NodeSchema.of(KIND, SEGMENTS);
    private final String packageSpecifier;

    public JvmClassTypeSignatureNode(
            TokenizableSource source,
            int start,
            int end,
            String packageSpecifier,
            List<JvmClassTypeSegmentNode> segments) {
        super(SCHEMA, source, start, end);
        this.packageSpecifier = packageSpecifier;
        segments.forEach(value -> addChild(SEGMENTS, value));
    }

    public String packageSpecifier() {
        return packageSpecifier;
    }

    public List<JvmClassTypeSegmentNode> segments() {
        return children(SEGMENTS);
    }

    public String internalName() {
        return packageSpecifier
                + segments().stream()
                        .map(JvmClassTypeSegmentNode::name)
                        .collect(Collectors.joining("$"));
    }

    @Override
    protected void validateStructure() {
        if (segments().isEmpty()) {
            throw new IllegalStateException("A class type requires a simple class type segment");
        }
    }
}
