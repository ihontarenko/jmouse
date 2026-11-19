package org.jmouse.java.parser.grammar.module;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.module.JavaModuleDeclarationNode;
import org.jmouse.java.parser.node.module.JavaModuleKind;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

public final class JavaModuleDeclarationParser implements ParseRule<JavaParsingInput, JavaModuleDeclarationNode> {
    @Override
    public JavaModuleDeclarationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.requireFeature(JavaSourceFeatures.MODULES);
        JavaModuleDeclarationNode node = new JavaModuleDeclarationNode(input.name(), input.point());
        JavaTypeParser.annotations(input, session).forEach(node::addAnnotation);
        node.setKind(input.take(JavaKeyword.OPEN, session) ? JavaModuleKind.OPEN : JavaModuleKind.NORMAL);
        input.expect(JavaKeyword.MODULE, session);
        node.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
        SourceSpan opening = input.point();
        input.expect(JavaToken.LEFT_BRACE, session);
        JavaSyntaxRecovery.readRegion(JavaModuleRoles.DIRECTIVE, input, session, opening,
                JavaRecoveryBoundaries.MEMBER, ParseProblem.Phase.STRUCTURE, node::addDirective);
        return input.finish(node, start);
    }
}
