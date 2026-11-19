package org.jmouse.java.parser.grammar.unit;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.unit.JavaPackageDeclarationNode;

public final class JavaPackageDeclarationParser implements ParseRule<JavaParsingInput, JavaPackageDeclarationNode> {
    @Override
    public JavaPackageDeclarationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaPackageDeclarationNode node = new JavaPackageDeclarationNode(input.name(), input.point());
        JavaTypeParser.annotations(input, session).forEach(node::addAnnotation);
        input.expect(JavaKeyword.PACKAGE, session);
        node.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
        input.expect(";", session);
        return input.finish(node, start);
    }
}
