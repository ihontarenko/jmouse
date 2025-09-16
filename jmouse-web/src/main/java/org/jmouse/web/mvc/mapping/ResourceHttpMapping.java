package org.jmouse.web.mvc.mapping;

import org.jmouse.core.MediaTypeFactory;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.AbstractHandlerPathMapping;
import org.jmouse.web.mvc.Route;
import org.jmouse.web.mvc.method.converter.MessageConverterManager;
import org.jmouse.web.mvc.resource.InternalResourceConfiguration;
import org.jmouse.web.mvc.resource.ResourceHandlerRegistry;
import org.jmouse.web.mvc.resource.ResourceHttpHandler;
import org.jmouse.web.mvc.resource.ResourceRegistration;

import java.util.List;

import static org.jmouse.web.mvc.Route.*;

/**
 * 🧷 Path mapping for serving static resources.
 *
 * <p>Registers HEAD/GET routes for ant-style URL patterns and keeps
 * associated {@link ResourceHttpHandler} handlers.</p>
 */
public class ResourceHttpMapping extends AbstractHandlerPathMapping<ResourceHttpHandler> {

    /**
     * ⚙️ Initialize mapping infrastructure and dependencies.
     *
     * @param context active {@link WebBeanContext}
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        PatternMatcherResourceLoader resourceLoader          = context.getBean(PatternMatcherResourceLoader.class);
        MediaTypeFactory             mediaTypeFactory        = context.getBean(MediaTypeFactory.class);
        MessageConverterManager      messageConverterManager = context.getBean(MessageConverterManager.class);
        ResourceHandlerRegistry      handlerRegistry         = context.getBean(ResourceHandlerRegistry.class);
        List<ResourceRegistration>   registrations           = handlerRegistry.getRegistrations();

        if (registrations == null || registrations.isEmpty()) {
            InternalResourceConfiguration internalConfiguration = context.getBean(InternalResourceConfiguration.class);
            registrations = internalConfiguration.getDefaultRegistrations();
        }

        for (ResourceRegistration registration : registrations) {
            ResourceHttpHandler handler = new ResourceHttpHandler(
                    registration, resourceLoader, messageConverterManager, mediaTypeFactory);

            handler.setCacheControl(registration.getCacheControl());
            handler.setCacheSeconds(registration.getCachePeriod() == null ? -1 : registration.getCachePeriod());
            handler.setVary(registration.getVary());
            handler.setSupportedMethods(registration.getAllow().toSupportedMethods());

            for (String pattern : registration.getPatterns()) {
                addHandlerMapping(GET(pattern), handler);
                addHandlerMapping(OPTIONS(pattern), handler);
                addHandlerMapping(HEAD(pattern), handler);
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
        return mapped instanceof ResourceHttpHandler;
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
    protected MethodParameter getReturnParameter(ResourceHttpHandler handler) {
        return null;
    }

}
