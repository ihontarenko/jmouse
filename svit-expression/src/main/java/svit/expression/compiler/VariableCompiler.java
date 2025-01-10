package svit.expression.compiler;

import svit.ast.compiler.Compiler;
import svit.ast.compiler.EvaluationContext;
import svit.expression.ast.VariableNode;

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
