package org.jmouse.action;

import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.InvocableMethod;
import org.jmouse.core.scope.Context;

import static org.jmouse.core.Verify.nonNull;

/**
 * Action-aware invocation request. ⚙️
 */
public record ActionInvocationRequest(
        InvocableMethod method,
        Context context,
        ActionRequest actionRequest
) implements InvocationRequest {

    public ActionInvocationRequest {
        nonNull(method, "method");
        nonNull(context, "context");
        nonNull(actionRequest, "actionRequest");
    }
}
