package svit.expression.compiler;

import svit.ast.compiler.Compiler;
import svit.ast.compiler.EvaluationContext;
import svit.ast.node.Node;
import svit.expression.EvaluationException;
import svit.expression.ast.ObjectMethodNode;
import svit.support.invocable.Invocable;
import svit.support.invocable.InvokeResult;
import svit.support.invocable.ObjectMethodInvocable;
import org.jmouse.core.reflection.Reflections;

import java.util.ArrayList;
import java.util.List;

public class MethodCompiler implements Compiler<ObjectMethodNode, Object> {

    @Override
    public Object compile(ObjectMethodNode node, EvaluationContext evaluationContext) {
        List<?> parameterValues = new ArrayList<>();
        Node    parametersNode  = node.getArguments();

        if (parametersNode != null && parametersNode.evaluate(evaluationContext) instanceof List<?> rawList) {
            parameterValues = rawList;
        }

        Class<?>[] parametersTypes = Reflections.getArgumentsTypes(parameterValues.toArray(Object[]::new));
        Object     object          = evaluationContext.requireVariable(node.getObjectName());
        Invocable  invocable       = new ObjectMethodInvocable(object, node.getMethodName(), parametersTypes);

        invocable.addParameters(parameterValues);

        InvokeResult result = invocable.invoke();

        if (result.hasErrors()) {
            throw new EvaluationException("UNABLE TO INVOKE OBJECT METHOD: %s".formatted(node.getMethodName()));
        }

        return result.getReturnValue();
    }

    @Override
    public Class<? extends ObjectMethodNode> nodeType() {
        return ObjectMethodNode.class;
    }

}
