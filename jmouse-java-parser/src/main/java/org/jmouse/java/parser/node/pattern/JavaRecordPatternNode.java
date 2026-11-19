package org.jmouse.java.parser.node.pattern;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed record-pattern syntax, with role-owned children in source order. */
public final class JavaRecordPatternNode extends JavaPatternNode {
    public static final NodeKind<JavaRecordPatternNode> KIND = new NodeKind<>(JavaNodeKindNames.RECORD_PATTERN, JavaRecordPatternNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaPatternNode> COMPONENTS = ChildRole.list(JavaChildRoleNames.COMPONENTS, JavaPatternNode.class);
    public static final NodeSchema<JavaRecordPatternNode> SCHEMA = NodeSchema.of(KIND, TYPE, COMPONENTS);

    public JavaRecordPatternNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaTypeSyntaxNode type() {
        return child(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
    }

    public List<JavaPatternNode> components() {
        return children(COMPONENTS);
    }

    public void addComponent(JavaPatternNode node) {
        addChild(COMPONENTS, node);
    }
}

