package org.jmouse.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.expression.ast.PathVariableNode;

public class PathVariableCompiler implements Compiler<PathVariableNode, String> {

    @Override
    public String compile(PathVariableNode node, EvaluationContext evaluationContext) {
        return node.getName();
    }

    @Override
    public Class<? extends PathVariableNode> nodeType() {
        return PathVariableNode.class;
    }

}
