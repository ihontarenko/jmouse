package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;

public interface ResourceComposerChain extends Chain<HttpServletRequest, ResourceQuery, String> {
    default String resolve(HttpServletRequest request, ResourceQuery resourceQuery) {
        return run(request, resourceQuery);
    }
}
