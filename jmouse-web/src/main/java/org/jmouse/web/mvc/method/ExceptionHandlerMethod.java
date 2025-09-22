package org.jmouse.web.mvc.method;

import java.lang.reflect.Method;

/**
 * 🎯 Represents a method that handles a specific {@link Throwable} type.
 *
 * Used to resolve and invoke an exception handler in controllers or global interceptor.
 *
 * <pre>{@code
 * @ExceptionHandler(IllegalArgumentException.class)
 * public ResponseEntity<?> handle(IllegalArgumentException ex) { ... }
 *
 * Method method = ...; // the handler method
 * ExceptionHandlerMethod eh = new ExceptionHandlerMethod(IllegalArgumentException.class, controller, method);
 * }</pre>
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public class ExceptionHandlerMethod extends HandlerMethod {

    private final Class<? extends Throwable> exceptionType;

    /**
     * 📌 Constructs a new exception handler method.
     *
     * @param exceptionType the exception this handler is bound to
     * @param bean the target bean (controller or interceptor)
     * @param method the handler method
     */
    public ExceptionHandlerMethod(Class<? extends Throwable> exceptionType, Object bean, Method method) {
        super(bean, method);
        this.exceptionType = exceptionType;
    }

    /**
     * 💥 The exception this method handles.
     *
     * @return exception type (subclass of {@link Throwable})
     */
    public Class<? extends Throwable> getExceptionType() {
        return exceptionType;
    }
}
