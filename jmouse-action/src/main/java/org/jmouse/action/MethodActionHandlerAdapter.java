package org.jmouse.action;

import org.jmouse.core.Verify;
import org.jmouse.core.invoke.InvocableMethod;
import org.jmouse.core.invoke.MethodInvoker;

/**
 * Adapts a method-backed handler to {@link ActionHandler}. 🎯
 */
public class MethodActionHandlerAdapter implements ActionHandler {

    private final InvocableMethod method;
    private final MethodInvoker   methodInvoker;

    public MethodActionHandlerAdapter(InvocableMethod method, MethodInvoker methodInvoker) {
        this.method = Verify.nonNull(method, "invocableMethod");
        this.methodInvoker = Verify.nonNull(methodInvoker, "methodInvoker");
    }

    @Override
    public Object handle(ActionRequest request) {
        return methodInvoker.invoke(
                new ActionInvocationRequest(
                        method,
                        request.context(),
                        request
                )
        );
    }
}