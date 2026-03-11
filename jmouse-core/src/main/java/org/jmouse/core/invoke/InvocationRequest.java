package org.jmouse.core.invoke;

import org.jmouse.core.Verify;
import org.jmouse.core.scope.Context;

/**
 * Runtime request for method invocation. ⚙️
 */
public interface InvocationRequest {

    /**
     * Returns target invocable method.
     */
    InvocableMethod method();

    /**
     * Returns invocation context.
     */
    Context context();

    /**
     * Default {@link InvocationRequest} implementation. 🧱
     */
    record Default(
            InvocableMethod method,
            Context context
    ) implements InvocationRequest {

        public Default {
            Verify.nonNull(method, "method");
            Verify.nonNull(context, "context");
        }
    }

}