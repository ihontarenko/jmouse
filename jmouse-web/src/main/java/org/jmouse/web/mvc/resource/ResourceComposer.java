package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;

public interface ResourceComposer extends Link<HttpServletRequest, ResourceQuery, String> {

    default Outcome<String> compose(
            HttpServletRequest request,
            ResourceQuery resourceQuery,
            ResourceComposerChain next
    ) {
        return handle(request, resourceQuery, next);
    }

}
