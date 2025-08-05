package org.jmouse.mvc.mapping;

import org.jmouse.mvc.AbstractHandlerPathMapping;
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
     * Initializes this mapping by scanning all {@link ControllerMethodRegistration}
     * beans in the current {@link WebBeanContext} and registering them.
     *
     * @param context current web application context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        for (ControllerMethodRegistration registration : WebBeanContext.getLocalBeans(
                ControllerMethodRegistration.class, context)) {
            addHandlerMapping(registration.route(), registration.controllerMethod());
        }
    }

    /**
     * ✅ Determines whether the given mapped handler is a {@link ControllerMethod}.
     *
     * <p>Used to check if this adapter or handler processor supports the
     * specified mapped handler during dispatching.
     *
     * @param mapped the handler object to inspect
     * @return {@code true} if the handler is a {@link ControllerMethod}, {@code false} otherwise
     */
    @Override
    public boolean supportsMappedHandler(Object mapped) {
        return mapped instanceof ControllerMethod;
    }
}
