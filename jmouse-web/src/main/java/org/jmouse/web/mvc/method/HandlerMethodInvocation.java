package org.jmouse.web.mvc.method;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.proxy.ProxyHelper;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.web.mvc.ArgumentResolverException;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.annotation.MethodDescription;
import org.jmouse.web.http.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * 🧠 Responsible for invoking a {@link HandlerMethod} with resolved arguments.
 *
 * <p>This class orchestrates argument resolution using registered {@link ArgumentResolver}s
 * and reflects the target method proxyInvocation on the associated controller bean.
 *
 * <p>Example:
 * <pre>{@code
 * HandlerMethodInvocation proxyInvocation = new HandlerMethodInvocation(method, mappingResult, resolvers);
 * Object result = proxyInvocation.invoke();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class HandlerMethodInvocation {

    private final static Logger LOGGER = LoggerFactory.getLogger(HandlerMethodInvocation.class);

    private final HandlerMethodContext   handlerContext;
    private final List<ArgumentResolver> argumentResolvers;
    private final MappingResult          mappingResult;
    private final AnnotationRepository   annotationRepository;

    public HandlerMethodInvocation(
            HandlerMethodContext handlerContext,
            MappingResult mappingResult,
            List<ArgumentResolver> resolvers
    ) {
        this.handlerContext = handlerContext;
        this.mappingResult = mappingResult;
        this.annotationRepository = AnnotationRepository.ofAnnotatedElement(getHandlerMethod().getMethod());
        this.argumentResolvers = resolvers;
    }

    /**
     * Returns the underlying {@link HandlerMethod}.
     *
     * @return handler method
     */
    public HandlerMethod getHandlerMethod() {
        return handlerContext.handlerMethod();
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
        HandlerMethod         handlerMethod  = handlerContext.handlerMethod();
        List<MethodParameter> parameters     = handlerMethod.getParameters();
        Object[]              arguments      = new Object[parameters.size()];
        Method                method         = handlerMethod.getMethod();
        RequestContext        requestContext = handlerContext.requestContext();

        for (MethodParameter parameter : parameters) {
            Object resolved = getArgumentResolver(parameter)
                    .resolveArgument(parameter, requestContext, mappingResult);
            arguments[parameter.getParameterIndex()] = resolved;
        }

        Optional<MergedAnnotation> annotation = annotationRepository.get(MethodDescription.class);

        if (annotation.isPresent()) {
            MethodDescription description = annotation.get().synthesize();
            MethodDescription.LOGGER.info("Endpoint Information: {}", description.value());
        }

        return invokeMethod(handlerMethod.getBean(), method, arguments);
    }

    /**
     * ⚡ Delegate the reflective method proxyInvocation.
     *
     * <p>Uses {@link ProxyHelper#invoke(Object, Method, Object[])} to transparently
     * handle both plain targets and proxy objects.</p>
     *
     * @param object    target bean (possibly proxied)
     * @param method    reflected method to call
     * @param arguments resolved arguments to pass
     * @return return value from the proxyInvocation
     * @throws HandlerMethodInvocationException if proxyInvocation fails at runtime
     */
    private Object invokeMethod(Object object, Method method, Object[] arguments) {
        return ProxyHelper.invoke(object, method, arguments);
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
                    "NO ARGUMENT RESOLVER!: %s".formatted(parameter));
        }

        return resolver;
    }

}
