package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.AbstractHandlerPathMapping;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.adapter.ControllerMethod;
import org.jmouse.web.context.WebBeanContext;

/**
 * 🧠 Maps {@link ControllerMethod} instances based on registered {@link ControllerMethodRegistration}.
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
public class ControllerMethodMapping extends AbstractHandlerPathMapping<ControllerMethod> {

    /**
     * Resolves the {@link ControllerMethod} handler for the incoming request.
     *
     * @param request current HTTP request
     * @return the matched handler, or {@code null} if no match found
     */
    @Override
    protected MappedHandler doGetHandler(HttpServletRequest request) {
        return getMappedHandler(request);
    }

    /**
     * Initializes this mapping by scanning all {@link ControllerMethodRegistration}
     * beans in the current {@link WebBeanContext} and registering them.
     *
     * @param context current web application context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        for (ControllerMethodRegistration registration : WebBeanContext.getLocalBeans(
                ControllerMethodRegistration.class, context)) {
            addHandlerMapping(registration.route(), registration.functionalRoute());
        }
    }
}
