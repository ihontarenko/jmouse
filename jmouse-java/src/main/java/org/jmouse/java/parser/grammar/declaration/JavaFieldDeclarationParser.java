package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.statement.JavaLocalVariableDeclarationParser;
import org.jmouse.java.parser.node.declaration.JavaFieldDeclarationNode;
import org.jmouse.java.parser.node.type.JavaInferredTypeNode;
import org.jmouse.java.parser.node.type.JavaVoidTypeNode;

public final class JavaFieldDeclarationParser implements ParseRule<JavaMemberHeader, JavaFieldDeclarationNode> {
    @Override
    public JavaFieldDeclarationNode parse(JavaMemberHeader header, ParseSession session) {
        JavaParsingInput input = header.declaration().input();
        if (!header.typeParameters().isEmpty() || header.type() instanceof JavaVoidTypeNode
                || header.type() instanceof JavaInferredTypeNode) {
            throw input.error("Invalid field declaration type or type parameters");
        }
        JavaFieldDeclarationNode node = new JavaFieldDeclarationNode(input.name(), input.point());
        node.setModifiers(header.declaration().modifiers());
        node.setType(header.type());
        node.addVariable(JavaLocalVariableDeclarationParser.variable(input, session, header.name()));
        while (input.take(",", session)) {
            node.addVariable(JavaLocalVariableDeclarationParser.variable(input, session));
        }
        input.expect(";", session);
        return input.finish(node, header.declaration().start());
    }
}
