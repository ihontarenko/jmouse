package svit.expression.compiler;

import svit.ast.compiler.Compiler;
import svit.ast.compiler.EvaluationContext;
import svit.expression.ast.ParameterNode;
import svit.expression.ast.ParametersNode;

import java.util.HashMap;
import java.util.Map;

public class ParametersCompiler implements Compiler<ParametersNode, Map<String, Object>> {

    @Override
    public Map<String, Object> compile(ParametersNode node, EvaluationContext evaluationContext) {
        Map<String, Object> parameters = new HashMap<>();

        for (ParameterNode parameter : node.getParameters()) {
            parameters.put((String) parameter.getKey().evaluate(evaluationContext), parameter.getValue().evaluate(
                    evaluationContext));
        }

        return parameters;
    }

    @Override
    public Class<? extends ParametersNode> nodeType() {
        return ParametersNode.class;
    }

}
