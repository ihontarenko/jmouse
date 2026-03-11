package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.jmouse.core.Verify.nonNull;

/**
 * Invokes bound methods. ▶️
 */
public interface MethodInvoker {

    /**
     * Invokes method for the given request.
     */
    <T> T invoke(InvocationRequest request);

    /**
     * Default reflection-based {@link MethodInvoker}. 🧱
     */
    class Default implements MethodInvoker {

        private final MethodArgumentResolver argumentResolver;

        public Default(MethodArgumentResolver argumentResolver) {
            this.argumentResolver = nonNull(argumentResolver, "argumentResolver");
        }

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