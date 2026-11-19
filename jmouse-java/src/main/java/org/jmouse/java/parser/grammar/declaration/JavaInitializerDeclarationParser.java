package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaInitializerDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaInitializerKind;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaModifierNode;

public final class JavaInitializerDeclarationParser
        implements ParseRule<JavaDeclarationHeader, JavaInitializerDeclarationNode> {
    @Override
    public JavaInitializerDeclarationNode parse(JavaDeclarationHeader header, ParseSession session) {
        JavaParsingInput input = header.input();
        JavaInitializerKind kind = JavaInitializerKind.INSTANCE;
        for (JavaSyntaxNode entry : header.modifiers().entries()) {
            if (!(entry instanceof JavaModifierNode modifier) || !modifier.keyword().spelling().equals(JavaKeyword.STATIC.spelling())
                    || kind == JavaInitializerKind.STATIC) {
                throw input.errorAt("An initializer accepts only one static modifier", entry.span());
            }
            kind = JavaInitializerKind.STATIC;
        }
        JavaInitializerDeclarationNode node = new JavaInitializerDeclarationNode(input.name(), input.point());
        node.setKind(kind);
        node.setBody(session.parse(JavaStatementRoles.BLOCK, input));
        return input.finish(node, header.start());
    }
}
