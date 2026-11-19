package org.jmouse.java.parser.node.expression;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed method-reference-expression syntax, with role-owned children in source order. */
public final class JavaMethodReferenceExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaMethodReferenceExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.METHOD_REFERENCE_EXPRESSION, JavaMethodReferenceExpressionNode.class);
    public static final ChildRole<JavaSyntaxNode> QUALIFIER = ChildRole.required(JavaChildRoleNames.QUALIFIER, JavaSyntaxNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE_ARGUMENTS = ChildRole.list(JavaChildRoleNames.TYPE_ARGUMENTS, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.optional(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final NodeSchema<JavaMethodReferenceExpressionNode> SCHEMA = NodeSchema.of(KIND, QUALIFIER, TYPE_ARGUMENTS, NAME);
    private JavaMethodReferenceKind kind;

    public JavaMethodReferenceExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaSyntaxNode qualifier() {
        return child(QUALIFIER);
    }

    public void setQualifier(JavaSyntaxNode node) {
        setChild(QUALIFIER, node);
    }

    public List<JavaTypeSyntaxNode> typeArguments() {
        return children(TYPE_ARGUMENTS);
    }

    public void addTypeArgument(JavaTypeSyntaxNode node) {
        addChild(TYPE_ARGUMENTS, node);
    }

    public Optional<JavaIdentifierNode> name() {
        return optionalChild(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public JavaMethodReferenceKind kind() {
        return kind;
    }

    public void setKind(JavaMethodReferenceKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

