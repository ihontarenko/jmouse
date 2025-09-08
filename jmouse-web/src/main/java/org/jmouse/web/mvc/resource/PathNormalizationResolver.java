package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;
import org.jmouse.web.http.HttpNormalizer;

public class PathNormalizationResolver extends AbstractResourceResolver {

    @Override
    public Outcome<Resource> handle(
            HttpServletRequest context, ResourceQuery resourceQuery, Chain<HttpServletRequest, ResourceQuery, Resource> next) {
        String requestPath = resourceQuery.path();
        String path        = HttpNormalizer.normalize(requestPath, false);

        // todo: temporary
        path = path.replace('_', '/');

        if (path == null || path.contains("..")) {
            return Outcome.done(null);
        }

        if (!path.equals(requestPath)) {
            return next.proceed(context, new ResourceQuery(path, resourceQuery.locations()));
        }

        return Outcome.next();
    }

}
