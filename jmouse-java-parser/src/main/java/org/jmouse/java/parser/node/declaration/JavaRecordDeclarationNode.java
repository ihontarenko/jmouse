package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.body.JavaTypeBodyNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed record-declaration syntax, with role-owned children in source order. */
public final class JavaRecordDeclarationNode extends JavaTypeDeclarationNode {
    public static final NodeKind<JavaRecordDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.RECORD_DECLARATION, JavaRecordDeclarationNode.class);
    public static final ChildRole<JavaRecordComponentNode> COMPONENTS = ChildRole.list(JavaChildRoleNames.COMPONENTS, JavaRecordComponentNode.class);
    public static final NodeSchema<JavaRecordDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, NAME, TYPE_PARAMETERS, COMPONENTS, INTERFACES, PERMITTED_TYPES, BODY);

    public JavaRecordDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaRecordComponentNode> components() {
        return children(COMPONENTS);
    }

    public void addComponent(JavaRecordComponentNode node) {
        addChild(COMPONENTS, node);
    }

}

