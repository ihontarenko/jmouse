package org.jmouse.web.mvc.adapter;


import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.Streamable;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.HandlerAdapter;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.mvc.RouteMatch;
import org.jmouse.web.mvc.resource.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public class ResourceHttpAdapter implements HandlerAdapter, InitializingBeanSupport<WebBeanContext> {

    private PatternMatcherResourceLoader loader;

    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof ResourceRegistration;
    }

    @Override
    public MVCResult handle(HttpServletRequest request, HttpServletResponse response, MappedHandler handler) {
        ResourceResolverChain chain = new SimpleResourceResolverChain(List.of(
                new PathNormalizationResolver(),
                new VersionalResourceResolver(),
                new LocationScanningResolver(loader)
        ));

        RouteMatch           match        = handler.mappingResult().match();
        ResourceRegistration registration = (ResourceRegistration) handler.handler();

        if (match != null && match.getVariable("filepath", null) instanceof String filepath) {
            List<Resource> locations = Streamable.of(registration.getLocations())
                    .map(loader::getResource).toList();
            Resource resource = chain.resolve(request, new ResourceQuery(filepath, locations));

            System.out.println(resource);
        }

        return null;
    }

    @Override
    public void afterCompletion(BeanContext context) {
        loader = context.getBean(PatternMatcherResourceLoader.class);
    }
}
