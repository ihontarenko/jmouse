package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaClassDeclarationNode;

/** Parses a class declaration after its modifier prefix. */
public final class JavaClassDeclarationParser implements ParseRule<JavaDeclarationHeader, JavaClassDeclarationNode> {
    @Override
    public JavaClassDeclarationNode parse(JavaDeclarationHeader header, ParseSession session) {
        JavaParsingInput input = header.input();
        input.expect(JavaKeyword.CLASS, session);
        JavaClassDeclarationNode node = new JavaClassDeclarationNode(input.name(), input.point());
        JavaTypeDeclarationParser.name(header, session, node);
        JavaTypeDeclarationParser.parameters(input, session, node::addTypeParameter);
        if (input.take(JavaKeyword.EXTENDS, session)) {
            node.setSuperclass(session.parse(JavaParseRoles.TYPE, input));
        }
        JavaTypeDeclarationParser.relations(input, session, JavaKeyword.IMPLEMENTS, node::addInterfaceType);
        if (input.matches(JavaKeyword.PERMITS)) {
            input.requireFeature(JavaSourceFeatures.SEALED_TYPES);
            JavaTypeDeclarationParser.relations(input, session, JavaKeyword.PERMITS, node::addPermittedType);
        }
        JavaTypeDeclarationParser.body(header, session, node, JavaTypeForm.CLASS);
        return node;
    }
}
