package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParameterListParser;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaConstructorDeclarationNode;

public final class JavaConstructorDeclarationParser
        implements ParseRule<JavaConstructorHeader, JavaConstructorDeclarationNode> {
    @Override
    public JavaConstructorDeclarationNode parse(JavaConstructorHeader header, ParseSession session) {
        JavaParsingInput input = header.declaration().input();
        JavaConstructorDeclarationNode node = new JavaConstructorDeclarationNode(input.name(), input.point());
        node.setModifiers(header.declaration().modifiers());
        header.typeParameters().forEach(node::addTypeParameter);
        node.setName(header.name());
        JavaParameterListParser.parse(input, session, node::addParameter, node::setReceiver);
        JavaTypeDeclarationParser.relations(input, session, JavaKeyword.THROWS, node::addThrownType);
        node.setBody(session.parse(JavaDeclarationRoles.CONSTRUCTOR_BODY, input));
        return input.finish(node, header.declaration().start());
    }
}
