package org.jmouse.web.mvc.resource;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.RoutePath;
import org.jmouse.web.mvc.mapping.ResourceHttpMapping;
import org.jmouse.web.mvc.routing.MappingRegistration;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceUrlResolver implements InitializingBeanSupport<WebBeanContext> {

    private Map<RoutePath, ResourceHttpHandler> mappings = new LinkedHashMap<>();

    public String lookupResourceUrl(String path) {
        ResourceHttpHandler handler     = null;
        RoutePath           matchedPath = null;

        for (Map.Entry<RoutePath, ResourceHttpHandler> entry : mappings.entrySet()) {
            if (entry.getKey().matches(path)) {
                handler = entry.getValue();
                matchedPath = entry.getKey();
                break;
            }
        }

        if (handler != null && matchedPath != null) {
            String                relativePath = matchedPath.extractPath(path);
            ResourceRegistration  registration = handler.getRegistration();
            ResourceQuery         query        = handler.getResourceQuery(relativePath, registration.getLocations());
            ResourceResolverChain chain        = handler.getResolverChain(
                    registration.getChainRegistration().getResolvers());

            System.out.println(chain);
        }

        return null;
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        ResourceHttpMapping resourceHttpMapping = context.getBean(ResourceHttpMapping.class);
        for (MappingRegistration<?> registration : resourceHttpMapping.getMappingRegistry()
                .getRegistrations()) {
            if (registration.handler() instanceof ResourceHttpHandler resourceHttpHandler) {
                RoutePath routePath = registration.criteria().getRoute().pathPattern();
                mappings.put(routePath, resourceHttpHandler);
            }
        }
    }

}
