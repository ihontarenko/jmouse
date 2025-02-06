package svit.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import svit.expression.ast.ValuesNode;

import java.util.List;

public class ValuesCompiler implements Compiler<ValuesNode, List<Object>> {

    @Override
    public List<Object> compile(ValuesNode node, EvaluationContext evaluationContext) {
        return node.getElements().stream().map(n -> n.evaluate(evaluationContext)).toList();
    }

    @Override
    public Class<? extends ValuesNode> nodeType() {
        return ValuesNode.class;
    }

}
