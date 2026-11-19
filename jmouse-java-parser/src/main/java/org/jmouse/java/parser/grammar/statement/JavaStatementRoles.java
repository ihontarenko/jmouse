package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.parser.grammar.JavaParseRoleNames;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.resource.JavaResourceNode;
import org.jmouse.java.parser.node.statement.JavaAssertStatementNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.node.statement.JavaBreakStatementNode;
import org.jmouse.java.parser.node.statement.JavaCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaCatchClauseNode;
import org.jmouse.java.parser.node.statement.JavaContinueStatementNode;
import org.jmouse.java.parser.node.statement.JavaDoWhileStatementNode;
import org.jmouse.java.parser.node.statement.JavaEmptyStatementNode;
import org.jmouse.java.parser.node.statement.JavaEnhancedForStatementNode;
import org.jmouse.java.parser.node.statement.JavaExpressionStatementNode;
import org.jmouse.java.parser.node.statement.JavaFinallyClauseNode;
import org.jmouse.java.parser.node.statement.JavaForStatementNode;
import org.jmouse.java.parser.node.statement.JavaIfStatementNode;
import org.jmouse.java.parser.node.statement.JavaLabeledStatementNode;
import org.jmouse.java.parser.node.statement.JavaLocalVariableDeclarationStatementNode;
import org.jmouse.java.parser.node.statement.JavaReturnStatementNode;
import org.jmouse.java.parser.node.statement.JavaStatementNode;
import org.jmouse.java.parser.node.statement.JavaSwitchBlockNode;
import org.jmouse.java.parser.node.statement.JavaSwitchStatementNode;
import org.jmouse.java.parser.node.statement.JavaSynchronizedStatementNode;
import org.jmouse.java.parser.node.statement.JavaThrowStatementNode;
import org.jmouse.java.parser.node.statement.JavaTryStatementNode;
import org.jmouse.java.parser.node.statement.JavaWhileStatementNode;
import org.jmouse.java.parser.node.statement.JavaYieldStatementNode;

/** Addressable statement productions; syntax dispatch is configured independently. */
public final class JavaStatementRoles {
    public static final ParseRole<JavaParsingInput, JavaSyntaxNode> SWITCH_ENTRY =
            role(JavaParseRoleNames.SWITCH_ENTRY, JavaSyntaxNode.class);
    public static final ParseRole<JavaParsingInput, JavaSwitchStatementNode> SWITCH_STATEMENT =
            role(JavaParseRoleNames.SWITCH_STATEMENT, JavaSwitchStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaSwitchBlockNode> SWITCH_BLOCK =
            role(JavaParseRoleNames.SWITCH_BLOCK, JavaSwitchBlockNode.class);
    public static final ParseRole<JavaParsingInput, JavaCaseLabelNode> CASE_LABEL =
            role(JavaParseRoleNames.CASE_LABEL, JavaCaseLabelNode.class);
    public static final ParseRole<JavaParsingInput, JavaStatementNode> FOR =
            role(JavaParseRoleNames.FOR, JavaStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaForStatementNode> FOR_STATEMENT =
            role(JavaParseRoleNames.FOR_STATEMENT, JavaForStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaEnhancedForStatementNode> ENHANCED_FOR_STATEMENT =
            role(JavaParseRoleNames.ENHANCED_FOR_STATEMENT, JavaEnhancedForStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaStatementNode> STATEMENT =
            role(JavaParseRoleNames.STATEMENT, JavaStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaCatchClauseNode> CATCH_CLAUSE =
            role(JavaParseRoleNames.CATCH_CLAUSE, JavaCatchClauseNode.class);
    public static final ParseRole<JavaParsingInput, JavaFinallyClauseNode> FINALLY_CLAUSE =
            role(JavaParseRoleNames.FINALLY_CLAUSE, JavaFinallyClauseNode.class);
    public static final ParseRole<JavaParsingInput, JavaResourceNode> RESOURCE =
            role(JavaParseRoleNames.RESOURCE, JavaResourceNode.class);
    public static final ParseRole<JavaParsingInput, JavaBlockNode> BLOCK =
            role(JavaParseRoleNames.BLOCK, JavaBlockNode.class);
    public static final ParseRole<JavaParsingInput, JavaIfStatementNode> IF_STATEMENT =
            role(JavaParseRoleNames.IF_STATEMENT, JavaIfStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaWhileStatementNode> WHILE_STATEMENT =
            role(JavaParseRoleNames.WHILE_STATEMENT, JavaWhileStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaDoWhileStatementNode> DO_WHILE_STATEMENT =
            role(JavaParseRoleNames.DO_WHILE_STATEMENT, JavaDoWhileStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaReturnStatementNode> RETURN_STATEMENT =
            role(JavaParseRoleNames.RETURN_STATEMENT, JavaReturnStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaThrowStatementNode> THROW_STATEMENT =
            role(JavaParseRoleNames.THROW_STATEMENT, JavaThrowStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaYieldStatementNode> YIELD_STATEMENT =
            role(JavaParseRoleNames.YIELD_STATEMENT, JavaYieldStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaBreakStatementNode> BREAK_STATEMENT =
            role(JavaParseRoleNames.BREAK_STATEMENT, JavaBreakStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaContinueStatementNode> CONTINUE_STATEMENT =
            role(JavaParseRoleNames.CONTINUE_STATEMENT, JavaContinueStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaAssertStatementNode> ASSERT_STATEMENT =
            role(JavaParseRoleNames.ASSERT_STATEMENT, JavaAssertStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaSynchronizedStatementNode> SYNCHRONIZED_STATEMENT =
            role(JavaParseRoleNames.SYNCHRONIZED_STATEMENT, JavaSynchronizedStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaLabeledStatementNode> LABELED_STATEMENT =
            role(JavaParseRoleNames.LABELED_STATEMENT, JavaLabeledStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaEmptyStatementNode> EMPTY_STATEMENT =
            role(JavaParseRoleNames.EMPTY_STATEMENT, JavaEmptyStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaTryStatementNode> TRY_STATEMENT =
            role(JavaParseRoleNames.TRY_STATEMENT, JavaTryStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaExpressionStatementNode> EXPRESSION_STATEMENT =
            role(JavaParseRoleNames.EXPRESSION_STATEMENT, JavaExpressionStatementNode.class);
    public static final ParseRole<JavaParsingInput, JavaLocalVariableDeclarationStatementNode> LOCAL_VARIABLE_DECLARATION =
            role(JavaParseRoleNames.LOCAL_VARIABLE_DECLARATION, JavaLocalVariableDeclarationStatementNode.class);

    private JavaStatementRoles() {
    }

    private static <N extends Node> ParseRole<JavaParsingInput, N> role(String name, Class<N> type) {
        return new ParseRole<>(name, JavaParsingInput.class, type);
    }
}
