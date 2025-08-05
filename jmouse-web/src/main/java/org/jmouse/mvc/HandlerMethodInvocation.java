package org.jmouse.mvc;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.mvc.mapping.annnotation.MethodDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 🧠 Responsible for invoking a {@link HandlerMethod} with resolved arguments.
 *
 * <p>This class orchestrates argument resolution using registered {@link ArgumentResolver}s
 * and reflects the target method invocation on the associated controller bean.
 *
 * <p>Example:
 * <pre>{@code
 * HandlerMethodInvocation invocation = new HandlerMethodInvocation(method, mappingResult, resolvers);
 * Object result = invocation.invoke();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @autho ihontarenko@gmail.com
 */
public class HandlerMethodInvocation {

    private final static Logger LOGGER = LoggerFactory.getLogger(HandlerMethodInvocation.class);

    private final List<ArgumentResolver> argumentResolvers;
    private final HandlerMethod          handlerMethod;
    private final MappingResult          mappingResult;
    private final InvocationResult       invocationResult;

    /**
     * Constructs a new invocation context for a handler method.
     *
     * @param handlerMethod      the target method and bean
     * @param mappingResult      matched route and metadata
     * @param argumentResolvers  available argument resolvers
     */
    public HandlerMethodInvocation(
            HandlerMethod handlerMethod,
            MappingResult mappingResult,
            InvocationResult invocationResult,
            List<ArgumentResolver> argumentResolvers
    ) {
        this.handlerMethod = handlerMethod;
        this.mappingResult = mappingResult;
        this.invocationResult = invocationResult;
        this.argumentResolvers = argumentResolvers;
    }

    /**
     * Returns the underlying {@link HandlerMethod}.
     *
     * @return handler method
     */
    public HandlerMethod getHandlerMethod() {
        return handlerMethod;
    }

    /**
     * Returns the mapping result including route match and metadata.
     *
     * @return the mapping result
     */
    public MappingResult getMappingResult() {
        return mappingResult;
    }

    /**
     * Resolves arguments and invokes the handler method.
     *
     * @return the return value of the invoked method
     * @throws RuntimeException if the method cannot be invoked
     */
    public Object invoke() {
        Object[]     arguments = {};
        List<Object> resolved  = new ArrayList<>();
        Method       method    = handlerMethod.getMethod();

        for (MethodParameter parameter : handlerMethod.getParameters()) {
            resolved.add(
                    getArgumentResolver(parameter).resolveArgument(parameter, mappingResult, invocationResult));
        }

        if (!resolved.isEmpty()) {
            arguments = resolved.toArray();
        }

        for (AnnotationDescriptor annotation : handlerMethod.getDescriptor().getAnnotations()) {
            if (annotation.unwrap().annotationType() == MethodDescription.class) {
                MethodDescription.LOGGER.info("Endpoint Information: {}", annotation.getAttribute("value"));
            }
        }

        return Reflections.invokeMethod(handlerMethod.getBean(), method, arguments);
    }

    /**
     * 🧩 Selects a suitable {@link ArgumentResolver} for the given method parameter.
     *
     * <p>Scans the available resolvers and returns the first one that supports the parameter.
     * Throws an {@link ArgumentResolverException} if no matching resolver is found.
     *
     * @param parameter the method parameter to resolve
     * @return the supporting {@link ArgumentResolver}
     * @throws ArgumentResolverException if no resolver supports the parameter
     */
    private ArgumentResolver getArgumentResolver(MethodParameter parameter) {
        ArgumentResolver resolver = null;

        for (ArgumentResolver argumentResolver : argumentResolvers) {
            if (argumentResolver.supportsParameter(parameter)) {
                resolver = argumentResolver;
                break;
            }
        }

        if (resolver == null) {
            throw new ArgumentResolverException(
                    "No argument resolver found for parameter: %s".formatted(parameter));
        }

        return resolver;
    }

}
