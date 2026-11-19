package org.jmouse.java.parser.grammar.declaration;

import java.util.Map;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;

/** Wires Java declaration rules into the shared jME registry. */
public final class JavaDeclarationGrammar {
    private JavaDeclarationGrammar() {
    }

    public static void install(ParseRuleRegistry rules) {
        Map<String, ParseRole<JavaDeclarationHeader, ? extends JavaTypeDeclarationNode>> types = Map.of(
                JavaKeyword.CLASS.spelling(), JavaDeclarationRoles.CLASS,
                JavaKeyword.INTERFACE.spelling(), JavaDeclarationRoles.INTERFACE,
                JavaKeyword.ENUM.spelling(), JavaDeclarationRoles.ENUM,
                JavaKeyword.RECORD.spelling(), JavaDeclarationRoles.RECORD,
                JavaToken.AT.spelling(), JavaDeclarationRoles.ANNOTATION_TYPE);
        rules.bind(JavaDeclarationRoles.TYPE_DECLARATION, JavaTypeDeclarationParser.class.getName(), new JavaTypeDeclarationParser());
        rules.bind(JavaDeclarationRoles.TYPE_HEADER, JavaTypeDeclarationParser.HeaderParser.class.getName(), new JavaTypeDeclarationParser.HeaderParser(types));
        rules.bind(JavaDeclarationRoles.CLASS, JavaClassDeclarationParser.class.getName(), new JavaClassDeclarationParser());
        rules.bind(JavaDeclarationRoles.INTERFACE, JavaInterfaceDeclarationParser.class.getName(), new JavaInterfaceDeclarationParser());
        rules.bind(JavaDeclarationRoles.ENUM, JavaEnumDeclarationParser.class.getName(), new JavaEnumDeclarationParser());
        rules.bind(JavaDeclarationRoles.RECORD, JavaRecordDeclarationParser.class.getName(), new JavaRecordDeclarationParser());
        rules.bind(JavaDeclarationRoles.ANNOTATION_TYPE, JavaAnnotationTypeDeclarationParser.class.getName(), new JavaAnnotationTypeDeclarationParser());
        rules.bind(JavaDeclarationRoles.TYPE_BODY, JavaTypeBodyParser.class.getName(), new JavaTypeBodyParser());
        rules.bind(JavaDeclarationRoles.MEMBER, JavaMemberParser.class.getName(), new JavaMemberParser(types.keySet()));
        rules.bind(JavaDeclarationRoles.METHOD, JavaMethodDeclarationParser.class.getName(), new JavaMethodDeclarationParser());
        rules.bind(JavaDeclarationRoles.FIELD, JavaFieldDeclarationParser.class.getName(), new JavaFieldDeclarationParser());
        rules.bind(JavaDeclarationRoles.ANNOTATION_ELEMENT, JavaAnnotationElementDeclarationParser.class.getName(), new JavaAnnotationElementDeclarationParser());
        rules.bind(JavaDeclarationRoles.CONSTRUCTOR, JavaConstructorDeclarationParser.class.getName(), new JavaConstructorDeclarationParser());
        rules.bind(JavaDeclarationRoles.COMPACT_CONSTRUCTOR, JavaCompactConstructorDeclarationParser.class.getName(), new JavaCompactConstructorDeclarationParser());
        rules.bind(JavaDeclarationRoles.INITIALIZER, JavaInitializerDeclarationParser.class.getName(), new JavaInitializerDeclarationParser());
        rules.bind(JavaDeclarationRoles.RECORD_COMPONENT, JavaRecordComponentParser.class.getName(), new JavaRecordComponentParser());
        rules.bind(JavaDeclarationRoles.ENUM_CONSTANT, JavaEnumConstantParser.class.getName(), new JavaEnumConstantParser());
        rules.bind(JavaDeclarationRoles.TYPE_PARAMETER, JavaTypeParameterParser.class.getName(), new JavaTypeParameterParser());
        rules.bind(JavaDeclarationRoles.BLOCK_ITEM, JavaBlockItemParser.class.getName(), new JavaBlockItemParser(types.keySet()));
        rules.bind(JavaDeclarationRoles.CONSTRUCTOR_BODY, JavaConstructorBodyParser.class.getName(), new JavaConstructorBodyParser());
        rules.bind(JavaParseRoles.ANONYMOUS_CLASS_BODY, JavaAnonymousClassBodyParser.class.getName(),
                new JavaAnonymousClassBodyParser());
    }
}
