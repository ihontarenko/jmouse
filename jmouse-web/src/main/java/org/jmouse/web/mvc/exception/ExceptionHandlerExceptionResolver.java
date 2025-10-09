package org.jmouse.web.mvc.exception;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.web.annotation.Controller;
import org.jmouse.web.annotation.ExceptionHandler;
import org.jmouse.core.Sorter;
import org.jmouse.web.mvc.AbstractExceptionResolver;
import org.jmouse.web.http.RequestAttributes;
import org.jmouse.web.http.RequestAttributesHolder;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.method.*;

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

    private ExceptionMappingRegistry exceptionMappings;

    /**
     * Resolvers for handler method arguments.
     */
    private List<ArgumentResolver> argumentResolvers;

    /**
     * Creates a new resolver.
     */
    public ExceptionHandlerExceptionResolver() { }

    /**
     * Returns whether this resolver supports the given exception.
     *
     * @param exception the exception to check
     * @return {@code true} if supported, {@code false} otherwise
     */
    @Override
    public boolean supportsException(Throwable exception) {
        return exceptionMappings.supportsException(exception);
    }

    /**
     * Registers a new exception handler method.
     *
     * @param exceptionType the type of exception it handles
     * @param bean          the controller bean instance
     * @param method        the handler method
     */
    public void addExceptionMapping(Class<? extends Throwable> exceptionType, Object bean, Method method) {
        exceptionMappings.addExceptionMapping(exceptionType, bean, method);
    }

    /**
     * Initializes this resolver by scanning the application context
     * for {@link Controller} beans and their {@link ExceptionHandler} methods.
     *
     * @param context the web application context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        List<ArgumentResolver> argumentResolvers = new ArrayList<>(
                context.getBeans(ArgumentResolver.class)
        );

        Sorter.sort(argumentResolvers);

        this.argumentResolvers = List.copyOf(argumentResolvers);
        this.exceptionMappings = context.getBean(ExceptionMappingRegistry.class);

        WebBeanContext.methodsOfAnnotatedClasses(Controller.class, this::initializeMethods, context);
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
     * @return an {@link MVCResult} containing the handler method result
     */
    @Override
    protected MVCResult doExceptionResolve(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        HandlerMethod handlerMethod = getExceptionHandler(exception.getClass());
        MappingResult mappingResult = mappedHandler == null ? null : mappedHandler.mappingResult();
        MVCResult     mvcResult     = null;

        if (handlerMethod != null) {
            HandlerMethodContext    context    = new HandlerMethodContext(requestContext, handlerMethod);
            HandlerMethodInvocation invocation = new HandlerMethodInvocation(context, mappingResult, argumentResolvers);
            MethodParameter         returnType = MethodParameter.forMethod(handlerMethod.getMethod(), -1);

            mvcResult = new MVCResult(null, returnType, mappedHandler);
            RequestAttributesHolder.setAttribute(RequestAttributes.MVC_RESULT_ATTRIBUTE, mvcResult);
            mvcResult.setReturnValue(invocation.invoke());
        }

        return mvcResult;
    }

    /**
     * Returns the registered handler method for the given exception type.
     *
     * @param exceptionType the exception class
     * @return the mapped handler method, or {@code null} if not registered
     */
    protected ExceptionHandlerMethod getExceptionHandler(Class<? extends Throwable> exceptionType) {
        return exceptionMappings.getExceptionHandler(exceptionType);
    }
}
