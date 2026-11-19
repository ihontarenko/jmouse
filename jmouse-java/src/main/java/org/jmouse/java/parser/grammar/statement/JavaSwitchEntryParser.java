package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.JavaGrammarContext;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.grammar.declaration.JavaDeclarationRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.statement.JavaCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaDefaultCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaNullCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaPatternCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaSwitchRuleNode;
import org.jmouse.java.parser.node.statement.JavaSwitchStatementGroupNode;
import org.jmouse.java.parser.source.ParseProblem;

import java.util.ArrayList;
import java.util.List;

/** One committed switch entry, replaceable independently from the enclosing switch body. */
public final class JavaSwitchEntryParser implements ParseRule<JavaParsingInput, JavaSyntaxNode> {
    @Override
    public JavaSyntaxNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        List<JavaCaseLabelNode> labels = labels(input, session);
        if (input.take("->", session)) {
            input.requireFeature(JavaSourceFeatures.SWITCH_EXPRESSIONS);
            JavaSwitchRuleNode rule = new JavaSwitchRuleNode(input.name(), input.point());
            labels.forEach(rule::addLabel);
            if (input.matches(JavaToken.LEFT_BRACE)) {
                rule.setOutcome(session.parse(JavaStatementRoles.BLOCK, input));
            } else if (input.matches(JavaKeyword.THROW)) {
                rule.setOutcome(session.parse(JavaStatementRoles.THROW_STATEMENT, input));
            } else {
                JavaExpressionNode expression = session.parse(JavaParseRoles.EXPRESSION, input);
                if (session.context(JavaGrammarContext.SWITCH_FORM)
                        == JavaGrammarContext.SwitchForm.STATEMENT) {
                    JavaExpressionStatementParser.requireStatementExpression(input, expression);
                }
                rule.setOutcome(expression);
                input.expect(JavaToken.SEMICOLON, session);
            }
            return input.finish(rule, start);
        }
        input.expect(":", session);
        JavaSwitchStatementGroupNode group =
                new JavaSwitchStatementGroupNode(input.name(), input.point());
        labels.forEach(group::addLabel);
        while (!input.matches(JavaKeyword.CASE)
                && !input.matches(JavaKeyword.DEFAULT)
                && !input.matches(JavaToken.RIGHT_BRACE)
                && !input.atEnd()) {
            JavaSyntaxRecovery.parse(
                            JavaDeclarationRoles.BLOCK_ITEM,
                            input,
                            session,
                            JavaRecoveryBoundaries.SWITCH_ENTRY,
                            ParseProblem.Phase.BODY)
                    .ifPresent(group::addStatement);
        }
        return input.finish(group, start);
    }

    private List<JavaCaseLabelNode> labels(JavaParsingInput input, ParseSession session) {
        List<JavaCaseLabelNode> labels = new ArrayList<>();
        int start = input.offset();
        if (input.take(JavaKeyword.DEFAULT, session)) {
            labels.add(new JavaDefaultCaseLabelNode(input.name(), input.spanFrom(start)));
            return labels;
        }
        input.expect(JavaKeyword.CASE, session);
        do {
            labels.add(session.parse(JavaStatementRoles.CASE_LABEL, input));
        } while (input.take(JavaToken.COMMA, session));
        validateCaseLabels(input, labels);
        return labels;
    }

    private void validateCaseLabels(JavaParsingInput input, List<JavaCaseLabelNode> labels) {
        if (labels.getFirst() instanceof JavaNullCaseLabelNode) {
            if (labels.size() > 2
                    || labels.size() == 2
                            && !(labels.getLast() instanceof JavaDefaultCaseLabelNode)) {
                throw input.error("A null case may only be followed by default");
            }
            return;
        }
        for (JavaCaseLabelNode label : labels) {
            if (label instanceof JavaDefaultCaseLabelNode
                    || label instanceof JavaNullCaseLabelNode) {
                throw input.error("The combined null/default case requires null first");
            }
            if (label instanceof JavaPatternCaseLabelNode && labels.size() != 1) {
                throw input.error("A pattern case must contain exactly one pattern");
            }
        }
    }
}
