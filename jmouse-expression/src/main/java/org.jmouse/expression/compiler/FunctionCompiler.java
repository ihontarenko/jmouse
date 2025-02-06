package svit.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import svit.expression.EvaluationException;
import svit.expression.ast.FunctionNode;
import org.jmouse.common.support.invocable.Invocable;
import org.jmouse.common.support.invocable.InvokeResult;
import org.jmouse.common.support.invocable.StaticMethodInvocable;

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
