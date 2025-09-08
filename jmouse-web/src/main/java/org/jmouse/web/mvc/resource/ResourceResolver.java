package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

public interface ResourceResolver extends Link<HttpServletRequest, ResourceQuery, Resource> {
    default Outcome<Resource> resolve(HttpServletRequest request, ResourceQuery resourceQuery, ResourceResolverChain next) {
        return handle(request, resourceQuery, next);
    }
}
