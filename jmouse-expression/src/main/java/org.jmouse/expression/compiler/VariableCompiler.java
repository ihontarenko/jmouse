package org.jmouse.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.expression.ast.VariableNode;

public class VariableCompiler implements Compiler<VariableNode, Object> {

    @Override
    public Object compile(VariableNode node, EvaluationContext evaluationContext) {
        return evaluationContext.requireVariable(node.getVariableName());
    }

    @Override
    public Class<? extends VariableNode> nodeType() {
        return VariableNode.class;
    }

}
