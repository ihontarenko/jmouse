package org.jmouse.action;

import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.InvocableMethod;
import org.jmouse.core.scope.Context;

import static org.jmouse.core.Verify.nonNull;

/**
 * {@link InvocationRequest} specialized for action execution. ⚙️
 *
 * <p>
 * Extends the generic invocation request with the associated
 * {@link ActionRequest}, allowing argument resolvers and handlers
 * to access action-specific data.
 * </p>
 */
public record ActionInvocationRequest(
        InvocableMethod method,
        Context context,
        ActionRequest actionRequest
) implements InvocationRequest {

    /**
     * Creates action invocation request.
     */
    public ActionInvocationRequest {
        nonNull(method, "method");
        nonNull(actionRequest, "actionRequest");
        nonNull(context, "context");
    }
}