package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;
import org.jmouse.util.PathHelper;

/**
 * 🧹 Resource resolver that normalizes incoming request paths.
 *
 * <ul>
 *   <li>Normalizes path using {@link PathHelper}</li>
 *   <li>Replaces underscores ({@code _}) with slashes ({@code /})</li>
 *   <li>Blocks suspicious paths containing {@code ..}</li>
 *   <li>Delegates with updated {@link ResourceQuery} if normalization changed the path</li>
 * </ul>
 */
public class PathNormalizationResolver extends AbstractResourceResolver {

    /**
     * 🔎 Normalize and sanitize resource path before continuing resolution.
     *
     * @param context       current HTTP request
     * @param resourceQuery resource query with raw path + locations
     * @param next          next chain element
     * @return outcome with:
     *         <ul>
     *           <li>{@link Outcome#done(Object)} — if path is invalid</li>
     *           <li>{@link Outcome#next()} — if unchanged, let chain continue</li>
     *           <li>delegated {@link ResourceQuery} — if path was normalized</li>
     *         </ul>
     */
    @Override
    public Outcome<Resource> handle(
            HttpServletRequest context,
            ResourceQuery resourceQuery,
            Chain<HttpServletRequest, ResourceQuery, Resource> next) {

        String requestPath = resourceQuery.path();
        String path        = PathHelper.normalize(requestPath, false);

        if (path != null) {
            path = path.replace('_', '/');
        }

        if (path == null || path.contains("..")) {
            return Outcome.done(null);
        }

        if (!path.equals(requestPath)) {
            return next.proceed(context, new ResourceQuery(path, resourceQuery.locations()));
        }

        return Outcome.next();
    }
}
