package svit.expression.compiler;

import svit.ast.compiler.Compiler;
import svit.ast.compiler.EvaluationContext;
import svit.expression.EvaluationException;
import svit.expression.ast.FunctionNode;
import svit.invocable.Invocable;
import svit.invocable.InvokeResult;
import svit.invocable.StaticMethodInvocable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FunctionCompiler implements Compiler<FunctionNode, Object> {

    @Override
    public Object compile(FunctionNode node, EvaluationContext evaluationContext) {
        List<?>   arguments = new ArrayList<>();
        Method    method    = evaluationContext.getFunction(node.getMethodName());
        Invocable invocable = new StaticMethodInvocable(method);

        if (node.getArguments().evaluate(evaluationContext) instanceof List<?> rawList) {
            arguments = rawList;
        }

        invocable.addParameters(arguments);

        InvokeResult result = invocable.invoke();

        if (result.hasErrors()) {
            throw new EvaluationException("UNABLE TO INVOKE STATIC METHOD: %s".formatted(node.getMethodName()));
        }

        return result.getReturnValue();
    }

    @Override
    public Class<? extends FunctionNode> nodeType() {
        return FunctionNode.class;
    }

}
