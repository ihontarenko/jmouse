package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaAnnotationTypeDeclarationNode;

/** Parses a annotationtype declaration after its modifier prefix. */
public final class JavaAnnotationTypeDeclarationParser implements ParseRule<JavaDeclarationHeader, JavaAnnotationTypeDeclarationNode> {
    @Override
    public JavaAnnotationTypeDeclarationNode parse(JavaDeclarationHeader header, ParseSession session) {
        JavaParsingInput input = header.input();
        input.expect("@", session);
        input.expect(JavaKeyword.INTERFACE, session);
        JavaAnnotationTypeDeclarationNode node = new JavaAnnotationTypeDeclarationNode(input.name(), input.point());
        JavaTypeDeclarationParser.name(header, session, node);
        
        JavaTypeDeclarationParser.body(header, session, node, JavaTypeForm.ANNOTATION);
        return node;
    }
}
