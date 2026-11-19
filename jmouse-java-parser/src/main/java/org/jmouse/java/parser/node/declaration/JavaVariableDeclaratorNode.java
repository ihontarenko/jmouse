package org.jmouse.java.parser.node.declaration;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed variable-declarator syntax, with role-owned children in source order. */
public final class JavaVariableDeclaratorNode extends JavaSyntaxNode {
    public static final NodeKind<JavaVariableDeclaratorNode> KIND = new NodeKind<>(JavaNodeKindNames.VARIABLE_DECLARATOR, JavaVariableDeclaratorNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaArrayDimensionNode> DIMENSIONS = ChildRole.list(JavaChildRoleNames.DIMENSIONS, JavaArrayDimensionNode.class);
    public static final ChildRole<JavaExpressionNode> INITIALIZER = ChildRole.optional(JavaChildRoleNames.INITIALIZER, JavaExpressionNode.class);
    public static final NodeSchema<JavaVariableDeclaratorNode> SCHEMA = NodeSchema.of(KIND, NAME, DIMENSIONS, INITIALIZER);

    public JavaVariableDeclaratorNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public List<JavaArrayDimensionNode> dimensions() {
        return children(DIMENSIONS);
    }

    public void addDimension(JavaArrayDimensionNode node) {
        addChild(DIMENSIONS, node);
    }

    public Optional<JavaExpressionNode> initializer() {
        return optionalChild(INITIALIZER);
    }

    public void setInitializer(JavaExpressionNode node) {
        setChild(INITIALIZER, node);
    }
}

