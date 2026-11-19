package org.jmouse.java.parser.projection;

import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.statement.JavaAssertStatementNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.node.statement.JavaBreakStatementNode;
import org.jmouse.java.parser.node.statement.JavaContinueStatementNode;
import org.jmouse.java.parser.node.statement.JavaDoWhileStatementNode;
import org.jmouse.java.parser.node.statement.JavaEmptyStatementNode;
import org.jmouse.java.parser.node.statement.JavaEnhancedForStatementNode;
import org.jmouse.java.parser.node.statement.JavaExpressionStatementNode;
import org.jmouse.java.parser.node.statement.JavaForStatementNode;
import org.jmouse.java.parser.node.statement.JavaIfStatementNode;
import org.jmouse.java.parser.node.statement.JavaLabeledStatementNode;
import org.jmouse.java.parser.node.statement.JavaLocalVariableDeclarationStatementNode;
import org.jmouse.java.parser.node.statement.JavaReturnStatementNode;
import org.jmouse.java.parser.node.statement.JavaSwitchStatementNode;
import org.jmouse.java.parser.node.statement.JavaSynchronizedStatementNode;
import org.jmouse.java.parser.node.statement.JavaThrowStatementNode;
import org.jmouse.java.parser.node.statement.JavaTryStatementNode;
import org.jmouse.java.parser.node.statement.JavaWhileStatementNode;
import org.jmouse.java.parser.node.statement.JavaYieldStatementNode;

final class JavaStandardStatementProjection {
    private JavaStandardStatementProjection() {}

    static void install(
            NodeOperationRegistry<JavaStatementProjectionContext, JavaStatementProjection>
                    registry) {
        registry.bind(
                JavaAssertStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaAssertStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaBlockNode.KIND,
                JavaStandardStatementProjection.class.getName() + "#" + JavaBlockNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaBreakStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaBreakStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaContinueStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaContinueStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaDoWhileStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaDoWhileStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaEmptyStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaEmptyStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaEnhancedForStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaEnhancedForStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaExpressionStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaExpressionStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaForStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaForStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaIfStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaIfStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaLabeledStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaLabeledStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaLocalVariableDeclarationStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaLocalVariableDeclarationStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaReturnStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaReturnStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaSwitchStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaSwitchStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaSynchronizedStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaSynchronizedStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaThrowStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaThrowStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaTryStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaTryStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaWhileStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaWhileStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaYieldStatementNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaYieldStatementNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
        registry.bind(
                JavaConstructorInvocationNode.KIND,
                JavaStandardStatementProjection.class.getName()
                        + "#"
                        + JavaConstructorInvocationNode.KIND.name(),
                (node, context) -> JavaStatementProjection.TRAVERSE);
    }
}
