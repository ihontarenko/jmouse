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
}
