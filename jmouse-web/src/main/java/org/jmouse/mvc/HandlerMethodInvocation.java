package org.jmouse.mvc;

import java.lang.reflect.InvocationTargetException;
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

    private final HandlerMethod          handlerMethod;
    private final MappingResult          mappingResult;
    private final List<ArgumentResolver> argumentResolvers;

    /**
     * Constructs a new invocation context for a handler method.
     *
     * @param handlerMethod      the target method and bean
     * @param mappingResult      matched route and metadata
     * @param argumentResolvers  available argument resolvers
     */
    public HandlerMethodInvocation(
            HandlerMethod handlerMethod, MappingResult mappingResult, List<ArgumentResolver> argumentResolvers) {
        this.handlerMethod = handlerMethod;
        this.mappingResult = mappingResult;
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

        for (MethodParameter parameter : handlerMethod.getParameters()) {
            for (ArgumentResolver argumentResolver : argumentResolvers) {
                // todo: throw if no supported
                if (argumentResolver.supportsParameter(parameter)) {
                    resolved.add(argumentResolver.resolveArgument(parameter, mappingResult));
                }
            }
        }

        if (!resolved.isEmpty()) {
            arguments = resolved.toArray();
        }

        Object returnValue;

        try {
            returnValue = handlerMethod.getMethod().invoke(handlerMethod.getBean(), arguments);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }
}
