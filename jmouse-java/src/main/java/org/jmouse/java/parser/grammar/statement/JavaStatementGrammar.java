package org.jmouse.java.parser.grammar.statement;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.statement.JavaStatementNode;

/** Installs standard implementations and validates declarative statement routing. */
public final class JavaStatementGrammar {
    private JavaStatementGrammar() {
    }

    public static void install(ParseRuleRegistry rules, Properties configuration) {
        Map<String, ParseRole<JavaParsingInput, ? extends JavaStatementNode>> available = new HashMap<>();
        rules.bind(JavaStatementRoles.SWITCH_STATEMENT, JavaSwitchStatementParser.class.getName(), new JavaSwitchStatementParser());
        rules.bind(JavaStatementRoles.SWITCH_BLOCK, JavaSwitchBlockParser.class.getName(), new JavaSwitchBlockParser());
        rules.bind(JavaStatementRoles.SWITCH_ENTRY, JavaSwitchEntryParser.class.getName(), new JavaSwitchEntryParser());
        rules.bind(JavaStatementRoles.CASE_LABEL, JavaCaseLabelParser.class.getName(), new JavaCaseLabelParser());
        available.put(JavaStatementRoles.SWITCH_STATEMENT.name(), JavaStatementRoles.SWITCH_STATEMENT);
        rules.bind(JavaStatementRoles.FOR_STATEMENT, JavaForStatementParser.class.getName(), new JavaForStatementParser());
        rules.bind(JavaStatementRoles.ENHANCED_FOR_STATEMENT, JavaEnhancedForStatementParser.class.getName(), new JavaEnhancedForStatementParser());
        rules.bind(JavaStatementRoles.FOR, JavaForParser.class.getName(), new JavaForParser());
        available.put(JavaStatementRoles.FOR.name(), JavaStatementRoles.FOR);
        rules.bind(JavaStatementRoles.BLOCK, JavaBlockParser.class.getName(), new JavaBlockParser());
        available.put(JavaStatementRoles.BLOCK.name(), JavaStatementRoles.BLOCK);
        rules.bind(JavaStatementRoles.IF_STATEMENT, JavaIfStatementParser.class.getName(), new JavaIfStatementParser());
        available.put(JavaStatementRoles.IF_STATEMENT.name(), JavaStatementRoles.IF_STATEMENT);
        rules.bind(JavaStatementRoles.WHILE_STATEMENT, JavaWhileStatementParser.class.getName(), new JavaWhileStatementParser());
        available.put(JavaStatementRoles.WHILE_STATEMENT.name(), JavaStatementRoles.WHILE_STATEMENT);
        rules.bind(JavaStatementRoles.DO_WHILE_STATEMENT, JavaDoWhileStatementParser.class.getName(), new JavaDoWhileStatementParser());
        available.put(JavaStatementRoles.DO_WHILE_STATEMENT.name(), JavaStatementRoles.DO_WHILE_STATEMENT);
        rules.bind(JavaStatementRoles.RETURN_STATEMENT, JavaReturnStatementParser.class.getName(), new JavaReturnStatementParser());
        available.put(JavaStatementRoles.RETURN_STATEMENT.name(), JavaStatementRoles.RETURN_STATEMENT);
        rules.bind(JavaStatementRoles.THROW_STATEMENT, JavaThrowStatementParser.class.getName(), new JavaThrowStatementParser());
        available.put(JavaStatementRoles.THROW_STATEMENT.name(), JavaStatementRoles.THROW_STATEMENT);
        rules.bind(JavaStatementRoles.YIELD_STATEMENT, JavaYieldStatementParser.class.getName(), new JavaYieldStatementParser());
        available.put(JavaStatementRoles.YIELD_STATEMENT.name(), JavaStatementRoles.YIELD_STATEMENT);
        rules.bind(JavaStatementRoles.BREAK_STATEMENT, JavaBreakStatementParser.class.getName(), new JavaBreakStatementParser());
        available.put(JavaStatementRoles.BREAK_STATEMENT.name(), JavaStatementRoles.BREAK_STATEMENT);
        rules.bind(JavaStatementRoles.CONTINUE_STATEMENT, JavaContinueStatementParser.class.getName(), new JavaContinueStatementParser());
        available.put(JavaStatementRoles.CONTINUE_STATEMENT.name(), JavaStatementRoles.CONTINUE_STATEMENT);
        rules.bind(JavaStatementRoles.ASSERT_STATEMENT, JavaAssertStatementParser.class.getName(), new JavaAssertStatementParser());
        available.put(JavaStatementRoles.ASSERT_STATEMENT.name(), JavaStatementRoles.ASSERT_STATEMENT);
        rules.bind(JavaStatementRoles.SYNCHRONIZED_STATEMENT, JavaSynchronizedStatementParser.class.getName(), new JavaSynchronizedStatementParser());
        available.put(JavaStatementRoles.SYNCHRONIZED_STATEMENT.name(), JavaStatementRoles.SYNCHRONIZED_STATEMENT);
        rules.bind(JavaStatementRoles.LABELED_STATEMENT, JavaLabeledStatementParser.class.getName(), new JavaLabeledStatementParser());
        available.put(JavaStatementRoles.LABELED_STATEMENT.name(), JavaStatementRoles.LABELED_STATEMENT);
        rules.bind(JavaStatementRoles.EMPTY_STATEMENT, JavaEmptyStatementParser.class.getName(), new JavaEmptyStatementParser());
        available.put(JavaStatementRoles.EMPTY_STATEMENT.name(), JavaStatementRoles.EMPTY_STATEMENT);
        rules.bind(JavaStatementRoles.TRY_STATEMENT, JavaTryStatementParser.class.getName(), new JavaTryStatementParser());
        available.put(JavaStatementRoles.TRY_STATEMENT.name(), JavaStatementRoles.TRY_STATEMENT);
        rules.bind(JavaStatementRoles.EXPRESSION_STATEMENT, JavaExpressionStatementParser.class.getName(), new JavaExpressionStatementParser());
        available.put(JavaStatementRoles.EXPRESSION_STATEMENT.name(), JavaStatementRoles.EXPRESSION_STATEMENT);
        rules.bind(JavaStatementRoles.LOCAL_VARIABLE_DECLARATION, JavaLocalVariableDeclarationParser.class.getName(), new JavaLocalVariableDeclarationParser());
        available.put(JavaStatementRoles.LOCAL_VARIABLE_DECLARATION.name(), JavaStatementRoles.LOCAL_VARIABLE_DECLARATION);

        rules.bind(JavaStatementRoles.RESOURCE, JavaResourceParser.class.getName(), new JavaResourceParser());
        rules.bind(JavaStatementRoles.CATCH_CLAUSE, JavaCatchClauseParser.class.getName(), new JavaCatchClauseParser());
        rules.bind(JavaStatementRoles.FINALLY_CLAUSE, JavaFinallyClauseParser.class.getName(), new JavaFinallyClauseParser());
        Map<String, ParseRole<JavaParsingInput, ? extends JavaStatementNode>> routes = new HashMap<>();
        String prefix = "statement.route.";
        for (String property : configuration.stringPropertyNames()) {
            if (property.startsWith(prefix)) {
                String identity = configuration.getProperty(property);
                ParseRole<JavaParsingInput, ? extends JavaStatementNode> role = available.get(identity);
                if (role == null) {
                    throw new IllegalArgumentException("Unknown statement route " + identity);
                }
                routes.put(property.substring(prefix.length()), role);
            }
        }
        rules.bind(JavaStatementRoles.STATEMENT, JavaStatementParser.class.getName(), new JavaStatementParser(routes));
    }
}
