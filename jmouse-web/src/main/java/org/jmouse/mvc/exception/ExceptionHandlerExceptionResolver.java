package org.jmouse.mvc.exception;

import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.*;
import org.jmouse.mvc.mapping.annotation.Controller;
import org.jmouse.mvc.mapping.annotation.ExceptionHandler;
import org.jmouse.util.Sorter;
import org.jmouse.web.context.WebBeanContext;

import java.lang.reflect.Method;
import java.util.*;

/**
 * ⚠️ {@code ExceptionHandlerExceptionResolver} resolves exceptions by invoking
 * controller methods annotated with {@link ExceptionHandler}.
 *
 * <p>This resolver scans beans annotated with {@link Controller} and collects
 * methods annotated with {@link ExceptionHandler}, registering them as handlers
 * for specific exception types.</p>
 *
 * <p>At runtime, when an exception is thrown during handler invocation, this resolver
 * checks if a matching handler exists and invokes it via {@link HandlerMethodInvocation}.</p>
 *
 * <p>This class supports multiple exception mappings and uses {@link ArgumentResolver}s
 * to resolve method arguments dynamically.</p>
 *
 * @author Ivan Hontarenko
 * @since 1.0
 */
public class ExceptionHandlerExceptionResolver extends AbstractExceptionResolver {

    /** Set of exception types this resolver can handle. */
    private final Set<Class<? extends Throwable>> supportedExceptions;

    /** Map of exception types to their corresponding handler methods. */
    private final Map<Class<? extends Throwable>, ExceptionHandlerMethod> exceptionMappings;

    /** Resolvers for handler method arguments. */
    private List<ArgumentResolver> argumentResolvers;

    /**
     * Creates a new resolver with empty exception mappings.
     */
    public ExceptionHandlerExceptionResolver() {
        this.supportedExceptions = new HashSet<>();
        this.exceptionMappings = new HashMap<>();
    }

    /**
     * Returns whether this resolver supports the given exception.
     *
     * @param exception the exception to check
     * @return {@code true} if supported, {@code false} otherwise
     */
    @Override
    public boolean supportsException(Throwable exception) {
        for (Class<? extends Throwable> supportedException : supportedExceptions) {
            if (supportedException.isAssignableFrom(exception.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a new exception handler method.
     *
     * @param exceptionType the type of exception it handles
     * @param bean          the controller bean instance
     * @param method        the handler method
     */
    public void addExceptionMapping(Class<? extends Throwable> exceptionType, Object bean, Method method) {
        supportedExceptions.add(exceptionType);
        exceptionMappings.put(exceptionType, new ExceptionHandlerMethod(exceptionType, bean, method));
    }

    /**
     * Initializes this resolver by scanning the application context
     * for {@link Controller} beans and their {@link ExceptionHandler} methods.
     *
     * @param context the web application context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        WebBeanContext.selectMethods(Controller.class, this::initializeMethods, context);

        List<ArgumentResolver> argumentResolvers = new ArrayList<>(
                WebBeanContext.getLocalBeans(ArgumentResolver.class, context)
        );

        Sorter.sort(argumentResolvers);

        this.argumentResolvers = List.copyOf(argumentResolvers);
    }

    /**
     * Processes all {@link ExceptionHandler}-annotated methods of a controller bean.
     *
     * @param methods the methods to process
     * @param bean    the controller bean
     */
    private void initializeMethods(Collection<Method> methods, Object bean) {
        for (Method method : methods) {
            AnnotationRepository       repository       = AnnotationRepository.ofAnnotatedElement(method);
            Optional<MergedAnnotation> exceptionHandler = repository.get(ExceptionHandler.class);
            if (exceptionHandler.isPresent()) {
                ExceptionHandler             annotation     = exceptionHandler.get().synthesize();
                Class<? extends Throwable>[] exceptionTypes = annotation.value();
                for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                    addExceptionMapping(exceptionType, bean, method);
                }
            }
        }
    }

    /**
     * Resolves an exception by invoking the corresponding handler method.
     *
     * @param requestContext the request context
     * @param mappedHandler  the original mapped handler
     * @param exception      the thrown exception
     * @return an {@link InvocationOutcome} containing the handler method result
     */
    @Override
    protected InvocationOutcome doExceptionResolve(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        InvocationOutcome outcome       = new Outcome(null);
        HandlerMethod     handlerMethod = getExceptionHandler(exception.getClass());
        MappingResult     mappingResult = mappedHandler == null ? null : mappedHandler.mappingResult();

        if (handlerMethod != null) {
            HandlerMethodContext    context    = new HandlerMethodContext(requestContext, handlerMethod);
            HandlerMethodInvocation invocation = new HandlerMethodInvocation(context, mappingResult,
                    outcome, argumentResolvers);
            outcome.setReturnValue(invocation.invoke());
            outcome.setReturnParameter(MethodParameter.forMethod(handlerMethod.getMethod(), -1));
        }

        return outcome;
    }

    /**
     * Returns the registered handler method for the given exception type.
     *
     * @param exceptionType the exception class
     * @return the mapped handler method, or {@code null} if not registered
     */
    protected ExceptionHandlerMethod getExceptionHandler(Class<? extends Throwable> exceptionType) {
        return exceptionMappings.get(exceptionType);
    }
}
