package org.jmouse.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.expression.ast.IdentifierNode;

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
