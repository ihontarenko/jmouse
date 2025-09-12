package org.jmouse.web.mvc.resource;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.RoutePath;
import org.jmouse.web.mvc.mapping.ResourceHttpMapping;
import org.jmouse.web.mvc.routing.MappingRegistration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🌐 Resolves resource URLs by applying registered {@link ResourceHttpHandler}s.
 *
 * <p>Iterates over configured {@link RoutePath} mappings to find
 * a handler capable of rewriting or versioning the requested URL.</p>
 *
 * <p>💡 Typically used in views (via {@code jMouseAsset()} EL function)
 * to generate cache-busting, versioned resource URLs.</p>
 */
public class ResourceUrlResolver implements InitializingBeanSupport<WebBeanContext> {

    /** 📌 Map of route paths to their corresponding resource handlers. */
    private final Map<RoutePath, ResourceHttpHandler> mappings = new LinkedHashMap<>();

    /**
     * 🔍 Look up a processed resource URL for the given raw path.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Find matching {@link RoutePath}</li>
     *   <li>Extract relative path from the match</li>
     *   <li>Build a {@link ResourceQuery} and resolver chain</li>
     *   <li>Compose the final URL via {@link ResourceResolverChain#compose}</li>
     * </ol>
     *
     * @param path original resource request path
     * @return resolved URL string, or {@code null} if no mapping found
     */
    public String lookupResourceUrl(String path) {
        ResourceHttpHandler handler = null;
        RoutePath           matched = null;

        for (Map.Entry<RoutePath, ResourceHttpHandler> entry : mappings.entrySet()) {
            if (entry.getKey().matches(path)) {
                handler = entry.getValue();
                matched = entry.getKey();
                break;
            }
        }

        if (handler != null && matched != null) {
            String                relativePath = matched.extractPath(path);
            String                pathMapping  = path.substring(0, path.indexOf(relativePath));
            ResourceRegistration  registration = handler.getRegistration();
            ResourceQuery         query        = handler.getResourceQuery(relativePath, registration.getLocations());
            ResourceResolverChain chain        = handler.getResolverChain(
                    registration.getChainRegistration().getResolvers());

            String resolvedPath = chain.compose(relativePath, new UrlComposerContext(path, null, query.locations()));

            if (resolvedPath != null) {
                return pathMapping + resolvedPath;
            }
        }

        return null;
    }

    /**
     * ⚙️ Initialize this resolver by scanning the {@link ResourceHttpMapping}
     * from the {@link WebBeanContext} and collecting all registered
     * {@link ResourceHttpHandler}s.
     *
     * @param context current web bean context
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        ResourceHttpMapping resourceHttpMapping = context.getBean(ResourceHttpMapping.class);
        for (MappingRegistration<?> registration : resourceHttpMapping.getMappingRegistry().getRegistrations()) {
            if (registration.handler() instanceof ResourceHttpHandler resourceHttpHandler) {
                RoutePath routePath = registration.criteria().getRoute().pathPattern();
                mappings.put(routePath, resourceHttpHandler);
            }
        }
    }
}
