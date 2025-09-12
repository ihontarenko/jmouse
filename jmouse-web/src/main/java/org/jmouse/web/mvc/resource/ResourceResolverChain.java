package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.io.Resource;

/**
 * ⛓️ Chain of {@link ResourceResolver}s for static resource lookup.
 *
 * <p>Executes resolvers in order until one produces a {@link Resource}
 * or the chain is exhausted.</p>
 */
public interface ResourceResolverChain extends Chain<HttpServletRequest, ResourceQuery, Resource> {

    /**
     * 🔎 Resolve a resource by delegating through the chain.
     *
     * @param request       current HTTP request
     * @param resourceQuery candidate lookup data
     * @return resolved {@link Resource}, or {@code null} if none found
     */
    default Resource resolve(HttpServletRequest request, ResourceQuery resourceQuery) {
        return run(request, resourceQuery);
    }

    /**
     * 🎼 Apply the composition chain to a resolved {@link Resource}.
     *
     * <p>Each resolver’s {@link ResourceComposer} may transform the
     * resource path (e.g. add versioning, rewrite URL, etc.).</p>
     *
     * @param relative      the relative path to compose final resource URL
     * @param context       the query context
     * @return the composed resource path, or {@code null} if no composer produced a value
     */
    String compose(String relative, UrlComposerContext context);

}
