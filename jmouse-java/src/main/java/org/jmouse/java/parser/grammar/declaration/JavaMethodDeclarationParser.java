package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParameterListParser;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.body.JavaBlockBodyNode;
import org.jmouse.java.parser.node.body.JavaSemicolonBodyNode;
import org.jmouse.java.parser.node.declaration.JavaMethodDeclarationNode;

public final class JavaMethodDeclarationParser implements ParseRule<JavaMemberHeader, JavaMethodDeclarationNode> {
    @Override
    public JavaMethodDeclarationNode parse(JavaMemberHeader header, ParseSession session) {
        JavaParsingInput input = header.declaration().input();
        JavaMethodDeclarationNode node = new JavaMethodDeclarationNode(input.name(), input.point());
        node.setModifiers(header.declaration().modifiers());
        header.typeParameters().forEach(node::addTypeParameter);
        node.setResultType(header.type());
        node.setName(header.name());
        JavaParameterListParser.parse(input, session, node::addParameter, node::setReceiver);
        JavaTypeParser.dimensions(input, session).forEach(node::addDimension);
        JavaTypeDeclarationParser.relations(input, session, JavaKeyword.THROWS, node::addThrownType);
        int bodyStart = input.offset();
        if (input.take(";", session)) {
            node.setBody(new JavaSemicolonBodyNode(input.name(), input.spanFrom(bodyStart)));
        } else {
            JavaBlockBodyNode body = new JavaBlockBodyNode(input.name(), input.point());
            body.setBlock(session.parse(JavaStatementRoles.BLOCK, input));
            node.setBody(input.finish(body, bodyStart));
        }
        return input.finish(node, header.declaration().start());
    }
}
