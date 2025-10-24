package org.jmouse.security.web.context;

import org.jmouse.security.SecurityContext;
import org.jmouse.web.http.RequestContextKeeper;

public class NullSecurityContextRepository implements SecurityContextRepository {

    @Override
    public SecurityContext load(RequestContextKeeper requestContext) {
        return SecurityContext.empty();
    }

    @Override
    public void save(SecurityContext context, RequestContextKeeper requestContext) {
        // NO-OP
    }

    @Override
    public void clear(SecurityContext context, RequestContextKeeper requestContext) {
        // NO-OP
    }

    @Override
    public boolean contains(SecurityContext context, RequestContextKeeper requestContext) {
        return false;
    }

}
