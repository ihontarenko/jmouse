package org.jmouse.java.parser.node.declaration;

import java.util.List;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed enum-declaration syntax, with role-owned children in source order. */
public final class JavaEnumDeclarationNode extends JavaTypeDeclarationNode {
    public static final NodeKind<JavaEnumDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.ENUM_DECLARATION, JavaEnumDeclarationNode.class);
    public static final NodeSchema<JavaEnumDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, NAME, TYPE_PARAMETERS, INTERFACES, PERMITTED_TYPES, BODY);

    public JavaEnumDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaEnumConstantNode> constants() {
        return body().constants();
    }

    public void addConstant(JavaEnumConstantNode node) {
        body().addConstant(node);
    }

}

