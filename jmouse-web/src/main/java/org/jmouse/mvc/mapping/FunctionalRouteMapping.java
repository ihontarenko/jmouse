package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.AbstractHandlerPathMapping;
import org.jmouse.mvc.adapter.FunctionalRoute;
import org.jmouse.web.context.WebBeanContext;

/**
 * 🧠 Maps {@link FunctionalRoute} instances based on registered {@link RouteRegistration}.
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
public class FunctionalRouteMapping extends AbstractHandlerPathMapping<FunctionalRoute> {

    /**
     * Resolves the {@link FunctionalRoute} handler for the incoming request.
     *
     * @param request current HTTP request
     * @return the matched handler, or {@code null} if no match found
     */
    @Override
    protected Object doGetHandler(HttpServletRequest request) {
        return getMappedHandler(request);
    }

    /**
     * Initializes this mapping by scanning all {@link RouteRegistration}
     * beans in the current {@link WebBeanContext} and registering them.
     *
     * @param context current web application context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        for (RouteRegistration r : WebBeanContext.getLocalBeans(
                RouteRegistration.class, context)) {
            addHandlerMapping(r.method(), r.route(), r.functionalRoute());
        }
    }
}
