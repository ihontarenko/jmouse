package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.io.Resource;

public interface ResourceResolverChain extends Chain<HttpServletRequest, ResourceQuery, Resource> {
    default Resource resolve(HttpServletRequest request, ResourceQuery resourceQuery) {
        return run(request, resourceQuery);
    }
}
