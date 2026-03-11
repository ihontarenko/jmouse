package org.jmouse.action;

import org.jmouse.core.Verify;
import org.jmouse.core.invoke.InvocableMethod;
import org.jmouse.core.invoke.MethodInvoker;

/**
 * {@link ActionHandler} adapter for method-backed actions. 🎯
 *
 * <p>
 * Wraps an {@link InvocableMethod} and delegates execution to
 * {@link MethodInvoker}. This allows action handlers to be
 * implemented as regular methods.
 * </p>
 */
public class MethodActionHandlerAdapter implements ActionHandler {

    private final InvocableMethod method;
    private final MethodInvoker   methodInvoker;

    /**
     * Creates adapter for the given method.
     *
     * @param method        invocable method
     * @param methodInvoker method invoker
     */
    public MethodActionHandlerAdapter(InvocableMethod method, MethodInvoker methodInvoker) {
        this.method = Verify.nonNull(method, "invocableMethod");
        this.methodInvoker = Verify.nonNull(methodInvoker, "methodInvoker");
    }

    /**
     * Invokes the underlying method to handle the action.
     */
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