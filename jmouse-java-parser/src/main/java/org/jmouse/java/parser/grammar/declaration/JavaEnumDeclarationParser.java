package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaEnumDeclarationNode;

/** Parses a enum declaration after its modifier prefix. */
public final class JavaEnumDeclarationParser implements ParseRule<JavaDeclarationHeader, JavaEnumDeclarationNode> {
    @Override
    public JavaEnumDeclarationNode parse(JavaDeclarationHeader header, ParseSession session) {
        JavaParsingInput input = header.input();
        input.expect(JavaKeyword.ENUM, session);
        JavaEnumDeclarationNode node = new JavaEnumDeclarationNode(input.name(), input.point());
        JavaTypeDeclarationParser.name(header, session, node);
        JavaTypeDeclarationParser.relations(input, session, JavaKeyword.IMPLEMENTS, node::addInterfaceType);
        JavaTypeDeclarationParser.body(header, session, node, JavaTypeForm.ENUM);
        return node;
    }
}
