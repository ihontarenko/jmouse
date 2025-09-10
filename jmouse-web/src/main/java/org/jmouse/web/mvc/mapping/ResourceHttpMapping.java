package org.jmouse.web.mvc.mapping;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.AbstractHandlerPathMapping;
import org.jmouse.web.mvc.AntPattern;
import org.jmouse.web.mvc.Route;
import org.jmouse.web.mvc.RoutePath;
import org.jmouse.web.mvc.resource.ResourceHandlerRegistry;
import org.jmouse.web.mvc.resource.ResourceRegistration;

/**
 * 🧷 Path mapping for serving static resources.
 *
 * <p>Registers HEAD/GET routes for ant-style URL patterns and keeps
 * associated {@link ResourceRegistration} handlers.</p>
 */
public class ResourceHttpMapping extends AbstractHandlerPathMapping<ResourceRegistration> {

    /**
     * ⚙️ Initialize mapping infrastructure and dependencies.
     *
     * @param context active {@link WebBeanContext}
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        for (ResourceRegistration registration : context.getBean(ResourceHandlerRegistry.class).getRegistrations()) {
            for (String pattern : registration.getPatterns()) {
                addHandlerMapping(Route.GET(pattern), registration);
                addHandlerMapping(Route.HEAD(pattern), registration);
            }
        }
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
