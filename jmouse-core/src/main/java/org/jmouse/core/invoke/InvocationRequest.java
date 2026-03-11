package org.jmouse.core.invoke;

import org.jmouse.core.Verify;
import org.jmouse.core.scope.Context;

/**
 * Describes a runtime request for method invocation. ⚙️
 *
 * <p>
 * Encapsulates the {@link InvocableMethod} to execute and the
 * associated {@link Context} used during argument resolution.
 * </p>
 */
public interface InvocationRequest {

    /**
     * Returns the target invocable method.
     *
     * @return invocable method
     */
    InvocableMethod method();

    /**
     * Returns invocation context.
     *
     * @return invocation context
     */
    Context context();

    /**
     * Default immutable {@link InvocationRequest} implementation. 🧱
     */
    record Default(InvocableMethod method, Context context) implements InvocationRequest {
        public Default {
            Verify.nonNull(method, "method");
            Verify.nonNull(context, "context");
        }
    }

}