package org.jmouse.java.analysis.jdk;

import org.jmouse.java.analysis.JavaNodeFacts;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodReferenceExpressionNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class JdkCoverageMapping {
    private JdkCoverageMapping() {}

    static List<JavaSyntaxNode> missing(JdkSourceIndex source) {
        Set<JavaSyntaxNode> missing = new LinkedHashSet<>();
        for (JavaSyntaxNode invocation : source.requiredInvocations) {
            JavaNodeFacts facts = source.facts.get(source.identities.get(invocation));
            if (facts == null || facts.invocation().isEmpty()) {
                missing.add(invocation);
            }
        }
        for (JavaSyntaxNode transfer : source.requiredControlTargets) {
            JavaNodeFacts facts = source.facts.get(source.identities.get(transfer));
            if (facts == null || facts.controlTarget().isEmpty()) {
                missing.add(transfer);
            }
        }
        for (JavaExpressionNode expression : source.requiredExpressions) {
            if (!source.facts.containsKey(source.identities.get(expression))) {
                missing.add(expression);
            }
            if ((expression instanceof JavaLambdaExpressionNode
                            || expression instanceof JavaMethodReferenceExpressionNode)
                    && !source.functionalTargets.containsKey(source.identities.get(expression))) {
                missing.add(expression);
            }
        }
        return List.copyOf(missing);
    }
}
