package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaAnnotationElementDeclarationNode;

public final class JavaAnnotationElementDeclarationParser
        implements ParseRule<JavaMemberHeader, JavaAnnotationElementDeclarationNode> {
    @Override
    public JavaAnnotationElementDeclarationNode parse(JavaMemberHeader header, ParseSession session) {
        JavaParsingInput input = header.declaration().input();
        if (!header.typeParameters().isEmpty()) {
            throw input.error("An annotation element cannot declare type parameters");
        }
        JavaAnnotationElementDeclarationNode node = new JavaAnnotationElementDeclarationNode(input.name(), input.point());
        node.setModifiers(header.declaration().modifiers());
        node.setType(header.type());
        node.setName(header.name());
        input.expect("(", session);
        input.expect(")", session);
        JavaTypeParser.dimensions(input, session).forEach(node::addDimension);
        if (input.take(JavaKeyword.DEFAULT, session)) {
            node.setDefaultValue(session.parse(JavaParseRoles.ANNOTATION_VALUE, input));
        }
        input.expect(";", session);
        return input.finish(node, header.declaration().start());
    }
}
