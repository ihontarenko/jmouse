package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

/**
 * 📂 Contract for resolving static resources.
 *
 * <p>Extends {@link Link} in a chain-of-responsibility:
 * each resolver may handle the request or delegate further.</p>
 */
public interface ResourceResolver extends Link<HttpServletRequest, ResourceQuery, Resource> {

    /**
     * 🔎 Attempt to resolve a resource for the given request.
     *
     * <ul>
     *   <li>Return {@link Outcome.Done} with a {@link Resource} if resolved</li>
     *   <li>Return {@link Outcome.Continue} to delegate to the next resolver</li>
     * </ul>
     *
     * @param request       current HTTP request
     * @param resourceQuery candidate lookup data
     * @param next          next resolver in the chain
     * @return outcome with resolved resource or continue signal
     */
    default Outcome<Resource> resolve(
            HttpServletRequest request,
            ResourceQuery resourceQuery,
            ResourceResolverChain next
    ) {
        return handle(request, resourceQuery, next);
    }
}
