package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.java.parser.grammar.JavaParseRoleNames;

import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.body.JavaConstructorBodySyntaxNode;
import org.jmouse.java.parser.node.body.JavaTypeBodyNode;
import org.jmouse.java.parser.node.declaration.JavaAnnotationElementDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaAnnotationTypeDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaClassDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaCompactConstructorDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaConstructorDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaEnumConstantNode;
import org.jmouse.java.parser.node.declaration.JavaEnumDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaFieldDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaInitializerDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaInterfaceDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaMethodDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.declaration.JavaRecordDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;

public final class JavaDeclarationRoles {
    public static final ParseRole<JavaParsingInput, JavaTypeDeclarationNode> TYPE_DECLARATION =
            new ParseRole<>(JavaParseRoleNames.TYPE_DECLARATION, JavaParsingInput.class, JavaTypeDeclarationNode.class);
    public static final ParseRole<JavaDeclarationHeader, JavaTypeDeclarationNode> TYPE_HEADER =
            new ParseRole<>(JavaParseRoleNames.TYPE_HEADER, JavaDeclarationHeader.class, JavaTypeDeclarationNode.class);
    public static final ParseRole<JavaDeclarationHeader, JavaClassDeclarationNode> CLASS =
            new ParseRole<>(JavaParseRoleNames.CLASS_DECLARATION, JavaDeclarationHeader.class, JavaClassDeclarationNode.class);
    public static final ParseRole<JavaDeclarationHeader, JavaInterfaceDeclarationNode> INTERFACE =
            new ParseRole<>(JavaParseRoleNames.INTERFACE_DECLARATION, JavaDeclarationHeader.class, JavaInterfaceDeclarationNode.class);
    public static final ParseRole<JavaDeclarationHeader, JavaEnumDeclarationNode> ENUM =
            new ParseRole<>(JavaParseRoleNames.ENUM_DECLARATION, JavaDeclarationHeader.class, JavaEnumDeclarationNode.class);
    public static final ParseRole<JavaDeclarationHeader, JavaRecordDeclarationNode> RECORD =
            new ParseRole<>(JavaParseRoleNames.RECORD_DECLARATION, JavaDeclarationHeader.class, JavaRecordDeclarationNode.class);
    public static final ParseRole<JavaDeclarationHeader, JavaAnnotationTypeDeclarationNode> ANNOTATION_TYPE =
            new ParseRole<>(JavaParseRoleNames.ANNOTATION_TYPE_DECLARATION, JavaDeclarationHeader.class, JavaAnnotationTypeDeclarationNode.class);
    public static final ParseRole<JavaParsingInput, JavaTypeBodyNode> TYPE_BODY =
            new ParseRole<>(JavaParseRoleNames.TYPE_BODY, JavaParsingInput.class, JavaTypeBodyNode.class);
    public static final ParseRole<JavaParsingInput, JavaDeclarationNode> MEMBER =
            new ParseRole<>(JavaParseRoleNames.MEMBER_DECLARATION, JavaParsingInput.class, JavaDeclarationNode.class);
    public static final ParseRole<JavaMemberHeader, JavaMethodDeclarationNode> METHOD =
            new ParseRole<>(JavaParseRoleNames.METHOD_DECLARATION, JavaMemberHeader.class, JavaMethodDeclarationNode.class);
    public static final ParseRole<JavaMemberHeader, JavaFieldDeclarationNode> FIELD =
            new ParseRole<>(JavaParseRoleNames.FIELD_DECLARATION, JavaMemberHeader.class, JavaFieldDeclarationNode.class);
    public static final ParseRole<JavaMemberHeader, JavaAnnotationElementDeclarationNode> ANNOTATION_ELEMENT =
            new ParseRole<>(JavaParseRoleNames.ANNOTATION_ELEMENT_DECLARATION, JavaMemberHeader.class, JavaAnnotationElementDeclarationNode.class);
    public static final ParseRole<JavaConstructorHeader, JavaConstructorDeclarationNode> CONSTRUCTOR =
            new ParseRole<>(JavaParseRoleNames.CONSTRUCTOR_DECLARATION, JavaConstructorHeader.class, JavaConstructorDeclarationNode.class);
    public static final ParseRole<JavaConstructorHeader, JavaCompactConstructorDeclarationNode> COMPACT_CONSTRUCTOR =
            new ParseRole<>(JavaParseRoleNames.COMPACT_CONSTRUCTOR_DECLARATION, JavaConstructorHeader.class, JavaCompactConstructorDeclarationNode.class);
    public static final ParseRole<JavaDeclarationHeader, JavaInitializerDeclarationNode> INITIALIZER =
            new ParseRole<>(JavaParseRoleNames.INITIALIZER_DECLARATION, JavaDeclarationHeader.class, JavaInitializerDeclarationNode.class);
    public static final ParseRole<JavaParsingInput, JavaRecordComponentNode> RECORD_COMPONENT =
            new ParseRole<>(JavaParseRoleNames.RECORD_COMPONENT, JavaParsingInput.class, JavaRecordComponentNode.class);
    public static final ParseRole<JavaParsingInput, JavaEnumConstantNode> ENUM_CONSTANT =
            new ParseRole<>(JavaParseRoleNames.ENUM_CONSTANT, JavaParsingInput.class, JavaEnumConstantNode.class);
    public static final ParseRole<JavaParsingInput, JavaTypeParameterNode> TYPE_PARAMETER =
            new ParseRole<>(JavaParseRoleNames.TYPE_PARAMETER, JavaParsingInput.class, JavaTypeParameterNode.class);
    public static final ParseRole<JavaParsingInput, JavaSyntaxNode> BLOCK_ITEM =
            new ParseRole<>(JavaParseRoleNames.BLOCK_ITEM, JavaParsingInput.class, JavaSyntaxNode.class);
    public static final ParseRole<JavaParsingInput, JavaConstructorBodySyntaxNode> CONSTRUCTOR_BODY =
            new ParseRole<>(JavaParseRoleNames.CONSTRUCTOR_BODY, JavaParsingInput.class, JavaConstructorBodySyntaxNode.class);

    private JavaDeclarationRoles() {
    }
}
