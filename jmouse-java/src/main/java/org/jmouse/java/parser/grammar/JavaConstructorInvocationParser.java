package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationKind;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

import java.util.ArrayList;
import java.util.List;

public final class JavaConstructorInvocationParser
        implements ParseRule<JavaParsingInput, JavaConstructorInvocationNode> {
    @Override
    public JavaConstructorInvocationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaConstructorInvocationNode node =
                new JavaConstructorInvocationNode(input.name(), input.point());
        List<JavaTypeSyntaxNode> arguments = new ArrayList<>();
        if (!input.matches(JavaKeyword.THIS)
                && !input.matches(JavaKeyword.SUPER)
                && !input.matches("<")) {
            node.setQualifier(
                    session.with(
                            JavaGrammarContext.PRIMARY_FORM,
                            JavaGrammarContext.PrimaryForm.CONSTRUCTOR_QUALIFIER,
                            () -> session.parse(JavaParseRoles.PRIMARY, input)));
            input.expect(".", session);
        }
        JavaTypeParser.explicitArguments(input, session, arguments::add);
        arguments.forEach(node::addTypeArgument);
        node.setKeywordSpan(input.point());
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

    static boolean qualifierEndAhead(JavaParsingInput input, ParseSession session) {
        if (!input.matches(".")) {
            return false;
        }
        if (input.cursor().lookAt(1).value().equals(JavaKeyword.SUPER.spelling())) {
            return true;
        }
        if (!input.cursor().lookAt(1).value().equals("<")) {
            return false;
        }
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        try {
            input.expect(".", session);
            JavaTypeParser.explicitArguments(input, session, ignored -> {});
            return input.matches(JavaKeyword.SUPER);
        } catch (JavaSyntaxException exception) {
            return false;
        } finally {
            input.restore(checkpoint);
        }
    }
}
