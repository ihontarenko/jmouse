package org.jmouse.web.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.AbstractHandlerPathMapping;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;
import org.jmouse.web.context.WebBeanContext;

/**
 * 🧠 Maps {@link RequestHttpHandler} instances based on registered {@link RequestHttpHandlerRegistration}.
 *
 * <p>Used to associate lambda-based handlers with route patterns at runtime.
 * Scans all local {@code FunctionalRouteRegistration} beans during initialization.
 *
 * <p>Example registration:
 * <pre>{@code
 * @Bean
 * FunctionalRouteRegistration helloRoute() {
 *     return new FunctionalRouteRegistration(
 *         route().GET("/hello"),
 *         (request, response) -> response.write("Hi!")
 *     );
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RequestHttpHandlerMapping extends AbstractHandlerPathMapping<RequestHttpHandler> {

    public static final MethodParameter METHOD_PARAMETER = MethodParameter.forMethod(
            -1, RequestHttpHandler.class, "handle", HttpServletRequest.class, HttpServletResponse.class);

    /**
     * Initializes this mapping by scanning all {@link RequestHttpHandlerRegistration}
     * beans in the current {@link WebBeanContext} and registering them.
     *
     * @param context current web application context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        for (RequestHttpHandlerRegistration registration : WebBeanContext.getLocalBeans(
                RequestHttpHandlerRegistration.class, context)) {
            addHandlerMapping(registration.route(), registration.handler());
        }
    }

    /**
     * ✅ Determines whether the given mapped handler is a {@link RequestHttpHandler}.
     *
     * <p>Used to check if this adapter or handler processor supports the
     * specified mapped handler during dispatching.
     *
     * @param mapped the handler object to inspect
     * @return {@code true} if the handler is a {@link RequestHttpHandler}, {@code false} otherwise
     */
    @Override
    public boolean supportsMappedHandler(Object mapped) {
        return mapped instanceof RequestHttpHandler;
    }

    /**
     * 🧩 Returns a pre-defined {@link MethodParameter} for the given handler.
     *
     * <p>This implementation always returns a static parameter, independent of the input.
     * Typically used when the target method parameter is known and constant.
     *
     * @param handler the controller method (ignored in this implementation)
     * @return the predefined {@code MethodParameter}
     */
    @Override
    protected MethodParameter getReturnParameter(RequestHttpHandler handler) {
        return METHOD_PARAMETER;
    }
}
