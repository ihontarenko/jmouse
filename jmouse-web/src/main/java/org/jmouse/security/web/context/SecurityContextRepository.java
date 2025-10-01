package org.jmouse.security.web.context;

import org.jmouse.security.core.SecurityContext;
import org.jmouse.web.http.request.RequestContext;

public interface SecurityContextRepository {

    SecurityContext load(RequestContext requestContext);

    void save(SecurityContext context, RequestContext requestContext);

    void clear(SecurityContext context, RequestContext requestContext);

    boolean contains(SecurityContext context, RequestContext requestContext);

}
