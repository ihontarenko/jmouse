package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

public class LocationScanningResolver extends AbstractResourceResolver {

    public LocationScanningResolver() { }

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
        Resource resource = root.merge(filepath);

        if (resource.isReadable()) {
            return resource;
        }

        return null;
    }

}
