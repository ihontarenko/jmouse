package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;

public class LocationScanningResolver extends AbstractResourceResolver {

    private final PatternMatcherResourceLoader loader;

    public LocationScanningResolver(PatternMatcherResourceLoader loader) {
        this.loader = loader;
    }

    @Override
    public Outcome<Resource> handle(HttpServletRequest context, ResourceQuery resourceQuery,
                          Chain<HttpServletRequest, ResourceQuery, Resource> next) {
        String clearPath = resourceQuery.path();

        for (Resource location : resourceQuery.locations()) {
            Resource resource = getResource(clearPath, location);

            if (resource != null) {
                return Outcome.done(resource);
            }
        }

        return Outcome.done(null);
    }

    private Resource getResource(String filepath, Resource root) {
        String   path     = root.getResourceName() + filepath;
        Resource resource = null;

        try {
            resource = loader.getResource(path);
        } catch (Exception ignored) {}

        return resource;
    }

}
