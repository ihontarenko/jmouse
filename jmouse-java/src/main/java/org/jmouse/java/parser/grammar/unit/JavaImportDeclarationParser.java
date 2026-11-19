package org.jmouse.java.parser.grammar.unit;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.unit.JavaImportDeclarationNode;
import org.jmouse.java.parser.node.unit.JavaImportKind;
import org.jmouse.java.parser.node.unit.JavaImportNode;

public final class JavaImportDeclarationParser implements ParseRule<JavaParsingInput, JavaImportNode> {
    @Override
    public JavaImportDeclarationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.IMPORT, session);
        JavaImportKind kind = input.take(JavaKeyword.STATIC, session)
                ? JavaImportKind.SINGLE_STATIC : JavaImportKind.SINGLE_TYPE;
        JavaImportDeclarationNode node = new JavaImportDeclarationNode(input.name(), input.point());
        node.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
        if (input.take(".", session)) {
            input.expect("*", session);
            kind = kind == JavaImportKind.SINGLE_STATIC ? JavaImportKind.STATIC_ON_DEMAND : JavaImportKind.TYPE_ON_DEMAND;
        }
        node.setKind(kind);
        input.expect(";", session);
        return input.finish(node, start);
    }
}
