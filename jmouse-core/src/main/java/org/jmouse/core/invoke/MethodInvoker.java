package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.jmouse.core.Verify.nonNull;

/**
 * Strategy for invoking bound methods. ▶️
 *
 * <p>
 * Responsible for resolving method arguments and executing the target method.
 * Implementations may use reflection, bytecode generation, or other invocation
 * mechanisms.
 * </p>
 */
public interface MethodInvoker {

    /**
     * Invokes a method using the provided request.
     *
     * @param request invocation request
     * @param <T>     expected return type
     *
     * @return invocation result
     */
    <T> T invoke(InvocationRequest request);

    /**
     * Default reflection-based {@link MethodInvoker}. 🧱
     *
     * <p>
     * Resolves method parameters via {@link MethodArgumentResolver}
     * and invokes the method using standard Java reflection.
     * </p>
     */
    class Default implements MethodInvoker {

        private final MethodArgumentResolver argumentResolver;

        /**
         * Creates invoker with the given argument resolver.
         *
         * @param argumentResolver argument resolver
         */
        public Default(MethodArgumentResolver argumentResolver) {
            this.argumentResolver = nonNull(argumentResolver, "argumentResolver");
        }

        /**
         * Resolves arguments and invokes the target method.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T invoke(InvocationRequest request) {
            InvocableMethod       method     = nonNull(request, "request").method();
            List<MethodParameter> parameters = method.getParameters();
            Object[]              arguments  = new Object[parameters.size()];

            for (int index = 0; index < parameters.size(); index++) {
                MethodParameter parameter = parameters.get(index);
                arguments[index] = argumentResolver.resolve(parameter, request);
            }

            try {
                return (T) method.getMethod().invoke(method.getTarget(), arguments);
            } catch (InvocationTargetException exception) {
                Throwable target = exception.getTargetException();

                if (target instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new IllegalStateException(
                        "Failed to invoke method '%s'.".formatted(method),
                        target
                );
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Failed to invoke method '%s'.".formatted(method),
                        exception
                );
            }
        }
    }

}