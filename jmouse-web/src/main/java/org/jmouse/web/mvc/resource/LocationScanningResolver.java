package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

import java.util.List;

/**
 * 📂 {@link ResourceResolver} that scans a list of base locations
 * to find the first readable resource matching the requested path.
 *
 * <p>Delegates resource URL composition to its nested {@link Composer}.</p>
 */
public class LocationScanningResolver extends AbstractResourceResolver {

    /**
     * 🏗️ Create a new resolver with an attached {@code LocationScanningResolver.Composer}.
     */
    public LocationScanningResolver() {
        super(null);
        setComposer(new Composer());
    }

    /**
     * 🔎 Try to locate the requested resource in configured locations.
     *
     * @param context       current HTTP request
     * @param resourceQuery lookup metadata (path + candidate locations)
     * @param next          next element in the resolver chain
     * @return {@link Outcome.Done} with resource if found, otherwise {@code null}
     */
    @Override
    public Outcome<Resource> handle(
            HttpServletRequest context,
            ResourceQuery resourceQuery,
            Chain<HttpServletRequest, ResourceQuery, Resource> next) {

        String relativePath = resourceQuery.path();
        Resource resource   = getResource(relativePath, resourceQuery.locations());

        if (resource != null) {
            return Outcome.done(resource);
        }

        return Outcome.done(null);
    }

    /**
     * 🔍 Iterate through candidate locations and resolve the first readable resource.
     *
     * @param relativePath requested path relative to base location
     * @param locations    list of base resource locations
     * @return resolved resource, or {@code null} if none found
     */
    private Resource getResource(String relativePath, List<? extends Resource> locations) {
        for (Resource location : locations) {
            Resource candidate = getResource(relativePath, location);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 🔎 Merge a relative path into a single base location.
     *
     * @param relativePath requested path
     * @param root         base resource location
     * @return readable resource, or {@code null} if not accessible
     */
    private Resource getResource(String relativePath, Resource root) {
        Resource resource = root.merge(relativePath);
        return resource.isReadable() ? resource : null;
    }

    /**
     * 🎼 Resource composer that ensures URL composition
     * only proceeds if the resource exists and is readable.
     */
    public class Composer implements ResourceComposer {

        /**
         * ▶️ Verify readability of resource and forward to the next composer.
         *
         * @param relativePath requested relative path
         * @param context      composer context containing request path, resource, and locations
         * @param next         next composer in the chain
         * @return outcome with composed path, or {@code null} if resource not found
         */
        @Override
        public Outcome<String> handle(
                String relativePath, UrlComposerContext context, Chain<String, UrlComposerContext, String> next) {

            Resource resource = LocationScanningResolver.this.getResource(relativePath, context.locations());

            if (resource != null && resource.isReadable()) {
                UrlComposerContext newContext =
                        new UrlComposerContext(context.requestPath(), resource, context.locations());
                return next.proceed(relativePath, newContext);
            }

            return Outcome.done(null);
        }
    }
}
