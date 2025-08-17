package org.jmouse.web.exception;

import org.jmouse.web.mvc.method.ExceptionHandlerMethod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 📌 Registry for mapping exception types to their corresponding handler methods.
 *
 * <p>Stores a set of supported exception types and provides lookup for
 * {@link ExceptionHandlerMethod} instances based on an exception class.</p>
 *
 * @author Ivan
 */
public class ExceptionMappingRegistry {

    /** 📂 Set of exception types this registry can handle. */
    private final Set<Class<? extends Throwable>> supportedExceptions;

    /** 🗂 Map of exception types to their corresponding handler method metadata. */
    private final Map<Class<? extends Throwable>, ExceptionHandlerMethod> exceptionMappings;

    /**
     * 🆕 Creates a new, empty exception mapping registry.
     */
    public ExceptionMappingRegistry() {
        this.supportedExceptions = new HashSet<>();
        this.exceptionMappings = new HashMap<>();
    }

    /**
     * ➕ Registers a mapping between an exception type and its handler method.
     *
     * @param exceptionType the exception type to handle
     * @param bean          the bean instance containing the handler method
     * @param method        the method that handles the exception
     */
    public void addExceptionMapping(Class<? extends Throwable> exceptionType, Object bean, Method method) {
        supportedExceptions.add(exceptionType);
        exceptionMappings.put(exceptionType, new ExceptionHandlerMethod(exceptionType, bean, method));
    }

    /**
     * 🔍 Retrieves the registered handler for the given exception type.
     *
     * @param exceptionType the exception class to look up
     * @return the matching {@link ExceptionHandlerMethod}, or {@code null} if not found
     */
    protected ExceptionHandlerMethod getExceptionHandler(Class<? extends Throwable> exceptionType) {
        return exceptionMappings.get(exceptionType);
    }

    /**
     * ✅ Checks if this registry supports the given exception.
     *
     * @param exception the exception instance to check
     * @return {@code true} if there is a registered handler, otherwise {@code false}
     */
    public boolean supportsException(Throwable exception) {
        for (Class<? extends Throwable> supportedException : supportedExceptions) {
            if (supportedException.equals(exception.getClass())) {
                return true;
            }
        }
        return false;
    }

}
