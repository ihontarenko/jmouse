package org.jmouse.expression.compiler;

import org.jmouse.common.ast.compiler.Compiler;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.common.ast.node.Node;
import org.jmouse.expression.EvaluationException;
import org.jmouse.expression.ast.ObjectMethodNode;
import org.jmouse.common.support.invocable.Invocable;
import org.jmouse.common.support.invocable.InvokeResult;
import org.jmouse.common.support.invocable.ObjectMethodInvocable;
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
