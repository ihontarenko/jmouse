package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaInterfaceDeclarationNode;

/** Parses a interface declaration after its modifier prefix. */
public final class JavaInterfaceDeclarationParser implements ParseRule<JavaDeclarationHeader, JavaInterfaceDeclarationNode> {
    @Override
    public JavaInterfaceDeclarationNode parse(JavaDeclarationHeader header, ParseSession session) {
        JavaParsingInput input = header.input();
        input.expect(JavaKeyword.INTERFACE, session);
        JavaInterfaceDeclarationNode node = new JavaInterfaceDeclarationNode(input.name(), input.point());
        JavaTypeDeclarationParser.name(header, session, node);
        JavaTypeDeclarationParser.parameters(input, session, node::addTypeParameter);
        JavaTypeDeclarationParser.relations(input, session, JavaKeyword.EXTENDS, node::addInterfaceType);
        if (input.matches(JavaKeyword.PERMITS)) {
            input.requireFeature(JavaSourceFeatures.SEALED_TYPES);
            JavaTypeDeclarationParser.relations(input, session, JavaKeyword.PERMITS, node::addPermittedType);
        }
        JavaTypeDeclarationParser.body(header, session, node, JavaTypeForm.INTERFACE);
        return node;
    }
}
