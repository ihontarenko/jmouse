package org.jmouse.web.mvc.mapping;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.AbstractHandlerPathMapping;
import org.jmouse.web.mvc.AntPattern;
import org.jmouse.web.mvc.Route;
import org.jmouse.web.mvc.resource.ResourceRegistration;

/**
 * 🧷 Path mapping for serving static resources.
 *
 * <p>Registers HEAD/GET routes for ant-style URL patterns and keeps
 * associated {@link ResourceRegistration} handlers.</p>
 */
public class ResourceHttpMapping extends AbstractHandlerPathMapping<ResourceRegistration> {

    /**
     * ➕ Register a static resource resolver for the given ant patterns.
     *
     * <ul>
     *   <li>Creates a single {@link ResourceRegistration}.</li>
     *   <li>Binds both {@code HEAD} and {@code GET} routes for each pattern.</li>
     *   <li>Collects underlying Ant matchers for fast checks.</li>
     * </ul>
     *
     * @param patterns ant-style URL patterns (e.g. {@code /assets/**}, {@code /img/*.png})
     * @return configured {@link ResourceRegistration}
     */
    public ResourceRegistration addResourceResolver(String... patterns) {
        ResourceRegistration registration = new ResourceRegistration();

        for (String pattern : patterns) {
            Route routeA = Route.HEAD(pattern);
            Route routeB = Route.GET(pattern);

            addHandlerMapping(routeA, registration);
            addHandlerMapping(routeB, registration);

            if (routeA.pathPattern() instanceof AntPattern patternA
                    && routeB.pathPattern() instanceof AntPattern patternB) {
                registration.addAntMatchers(patternA.getAntMatcher(), patternB.getAntMatcher());
            }
        }

        return registration;
    }

    /**
     * ⚙️ Initialize mapping infrastructure and dependencies.
     *
     * @param context active {@link WebBeanContext}
     */
    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    /**
     * ✅ Check whether this mapping supports the given mapped handler object.
     *
     * @param mapped candidate mapped handler
     * @return {@code true} if supported by this mapping
     */
    @Override
    public boolean supportsMappedHandler(Object mapped) {
        return mapped instanceof ResourceRegistration;
    }

    /**
     * 🏷️ Return type metadata for the given resource handler.
     *
     * <p>Used by the MVC pipeline to decide how to process the handler's result.</p>
     *
     * @param handler resource registration
     * @return method parameter describing the return type
     * @throws UnsupportedOperationException if not implemented yet
     */
    @Override
    protected MethodParameter getReturnParameter(ResourceRegistration handler) {
        return null;
    }
}
