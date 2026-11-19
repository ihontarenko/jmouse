package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;

import java.util.List;

public final class JvmClassTypeSegmentNode extends JvmTextNode {
    public static final NodeKind<JvmClassTypeSegmentNode> KIND =
            new NodeKind<>(JvmTextNames.CLASS_TYPE_SEGMENT, JvmClassTypeSegmentNode.class);
    public static final ChildRole<JvmTypeArgumentNode> ARGUMENTS =
            ChildRole.list(JvmTextNames.ARGUMENTS, JvmTypeArgumentNode.class);
    public static final NodeSchema<JvmClassTypeSegmentNode> SCHEMA = NodeSchema.of(KIND, ARGUMENTS);
    private final String name;

    public JvmClassTypeSegmentNode(
            TokenizableSource source,
            int start,
            int end,
            String name,
            List<JvmTypeArgumentNode> arguments) {
        super(SCHEMA, source, start, end);
        this.name = name;
        arguments.forEach(value -> addChild(ARGUMENTS, value));
    }

    public String name() {
        return name;
    }

    public List<JvmTypeArgumentNode> arguments() {
        return children(ARGUMENTS);
    }
}
