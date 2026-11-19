package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;

/** A conditional-expression context excludes assignment and an unparenthesized lambda prefix. */
public final class JavaConditionalExpressionParser implements ParseRule<JavaParsingInput, JavaExpressionNode> {
    private final JavaExpressionParser expressions;

    public JavaConditionalExpressionParser(JavaOperatorCatalog operators, int minimum, int prefix) {
        expressions = new JavaExpressionParser(operators, minimum, prefix);
    }

    @Override
    public JavaExpressionNode parse(JavaParsingInput input, ParseSession session) {
        return session.with(JavaGrammarContext.EXPRESSION_FORM, JavaGrammarContext.ExpressionForm.CONDITIONAL,
                () -> expressions.parse(input, session));
    }
}
