package org.jmouse.security.web.context;

import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.core.SecurityContext;
import org.jmouse.web.http.request.RequestContext;

public class InMemorySecurityContextRepository implements SecurityContextRepository {

    @Override
    public SecurityContext load(RequestContext requestContext) {
        return SecurityContextHolder.getContext();
    }

    @Override
    public void save(SecurityContext context, RequestContext requestContext) {
        SecurityContextHolder.setContext(context);
    }

    @Override
    public void clear(SecurityContext context, RequestContext requestContext) {
        SecurityContextHolder.clearContext();
    }

    @Override
    public boolean contains(SecurityContext context, RequestContext requestContext) {
        return context.getAuthentication() != null;
    }

}
