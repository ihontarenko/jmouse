package org.jmouse.security.web.context;

import org.jmouse.security.core.SecurityContext;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.RequestContextKeeper;

public interface SecurityContextRepository {

    SecurityContext load(RequestContextKeeper requestContext);

    void save(SecurityContext context, RequestContextKeeper requestContext);

    void clear(SecurityContext context, RequestContextKeeper requestContext);

    boolean contains(SecurityContext context, RequestContextKeeper requestContext);

}
