package org.jmouse.java.parser.grammar;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationKind;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.expression.JavaNameExpressionNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

public final class JavaConstructorInvocationParser implements ParseRule<JavaParsingInput, JavaConstructorInvocationNode> {
    @Override
    public JavaConstructorInvocationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaConstructorInvocationNode node = new JavaConstructorInvocationNode(input.name(), input.point());
        List<JavaTypeSyntaxNode> arguments = new ArrayList<>();
        if (!input.matches(JavaKeyword.THIS) && !input.matches(JavaKeyword.SUPER) && !input.matches("<")) {
            JavaNameExpressionNode qualifier = new JavaNameExpressionNode(input.name(), input.point());
            qualifier.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
            input.finish(qualifier, start);
            node.setQualifier(qualifier);
            input.expect(".", session);
        }
        JavaTypeParser.explicitArguments(input, session, arguments::add);
        arguments.forEach(node::addTypeArgument);
        if (input.take(JavaKeyword.THIS, session)) {
            if (node.qualifier().isPresent()) {
                throw input.error("An explicit this constructor invocation cannot be qualified");
            }
            node.setKind(JavaConstructorInvocationKind.THIS);
        } else {
            input.expect(JavaKeyword.SUPER, session);
            node.setKind(JavaConstructorInvocationKind.SUPER);
        }
        JavaArgumentListParser.parse(input, session, node::addArgument);
        input.expect(";", session);
        return input.finish(node, start);
    }
}
