package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

import java.util.List;

public class LocationScanningResolver extends AbstractResourceResolver {

    public LocationScanningResolver() {
        super(null);
        setComposer(new Composer());
    }

    @Override
    public Outcome<Resource> handle(HttpServletRequest context, ResourceQuery resourceQuery,
                          Chain<HttpServletRequest, ResourceQuery, Resource> next) {
        String   relativePath = resourceQuery.path();
        Resource resource     = getResource(relativePath, resourceQuery.locations());

        if (resource != null) {
            return Outcome.done(resource);
        }

        return Outcome.done(null);
    }

    private Resource getResource(String relativePath, List<? extends Resource> locations) {
        Resource resource = null;

        for (Resource location : locations) {
            Resource candidate = getResource(relativePath, location);
            if (candidate != null) {
                resource = candidate;
                break;
            }
        }

        return resource;
    }

    private Resource getResource(String relativePath, Resource root) {
        Resource resource = root.merge(relativePath);

        if (resource.isReadable()) {
            return resource;
        }

        return null;
    }

    public class Composer implements ResourceComposer {

        @Override
        public Outcome<String> handle(
                String relativePath, UrlComposerContext context, Chain<String, UrlComposerContext, String> next) {
            Resource resource = LocationScanningResolver.this.getResource(relativePath, context.locations());

            if (resource != null && resource.isReadable()) {
                return next.proceed(relativePath, new UrlComposerContext(resource, context.locations()));
            }

            return Outcome.done(null);
        }

    }

}
