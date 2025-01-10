package svit.expression.compiler;

import svit.ast.compiler.Compiler;
import svit.ast.compiler.EvaluationContext;
import svit.expression.ast.IdentifierNode;

public class IdentifierCompiler implements Compiler<IdentifierNode, String> {

    @Override
    public String compile(IdentifierNode node, EvaluationContext evaluationContext) {
        return node.getIdentifier();
    }

    @Override
    public Class<? extends IdentifierNode> nodeType() {
        return IdentifierNode.class;
    }

}
