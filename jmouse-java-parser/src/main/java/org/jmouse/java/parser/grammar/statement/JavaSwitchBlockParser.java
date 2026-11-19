package org.jmouse.java.parser.grammar.statement;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.declaration.JavaDeclarationRoles;
import org.jmouse.java.parser.grammar.JavaGrammarContext;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.statement.JavaCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaDefaultCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaSwitchBlockNode;
import org.jmouse.java.parser.node.statement.JavaSwitchRuleNode;
import org.jmouse.java.parser.node.statement.JavaSwitchStatementGroupNode;

/** One switch body preserves either arrow rules or colon groups in source order. */
public final class JavaSwitchBlockParser implements ParseRule<JavaParsingInput, JavaSwitchBlockNode> {
    private enum LabelStyle { RULE, GROUP }

    @Override
    public JavaSwitchBlockNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect("{", session);
        JavaSwitchBlockNode node = new JavaSwitchBlockNode(input.name(), input.point());
        LabelStyle style = null;
        while (!input.take("}", session)) {
            int entryStart = input.offset();
            List<JavaCaseLabelNode> labels = labels(input, session);
            if (input.take("->", session)) {
                if (style == LabelStyle.GROUP) {
                    throw input.error("Arrow rules and colon groups cannot be mixed");
                }
                style = LabelStyle.RULE;
                input.requireFeature(JavaSourceFeatures.SWITCH_EXPRESSIONS);
                JavaSwitchRuleNode rule = new JavaSwitchRuleNode(input.name(), input.point());
                labels.forEach(rule::addLabel);
                if (input.matches("{")) {
                    rule.setOutcome(session.parse(JavaStatementRoles.BLOCK, input));
                } else if (input.matches(JavaKeyword.THROW)) {
                    rule.setOutcome(session.parse(JavaStatementRoles.THROW_STATEMENT, input));
                } else {
                    JavaExpressionNode expression = session.parse(JavaParseRoles.EXPRESSION, input);
                    if (session.context(JavaGrammarContext.SWITCH_FORM) == JavaGrammarContext.SwitchForm.STATEMENT) {
                        JavaExpressionStatementParser.requireStatementExpression(input, expression);
                    }
                    rule.setOutcome(expression);
                    input.expect(";", session);
                }
                node.addEntry(input.finish(rule, entryStart));
            } else {
                if (style == LabelStyle.RULE) {
                    throw input.error("Arrow rules and colon groups cannot be mixed");
                }
                style = LabelStyle.GROUP;
                input.expect(":", session);
                JavaSwitchStatementGroupNode group = new JavaSwitchStatementGroupNode(input.name(), input.point());
                labels.forEach(group::addLabel);
                while (!input.matches(JavaKeyword.CASE) && !input.matches(JavaKeyword.DEFAULT) && !input.matches("}")) {
                    group.addStatement(session.parse(JavaDeclarationRoles.BLOCK_ITEM, input));
                }
                node.addEntry(input.finish(group, entryStart));
            }
        }
        return input.finish(node, start);
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
        } while (input.take(",", session));
        return labels;
    }
}
