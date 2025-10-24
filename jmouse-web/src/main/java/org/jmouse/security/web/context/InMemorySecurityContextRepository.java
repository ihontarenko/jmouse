package org.jmouse.security.web.context;

import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.SecurityContext;
import org.jmouse.web.http.RequestContextKeeper;

public class InMemorySecurityContextRepository implements SecurityContextRepository {

    @Override
    public SecurityContext load(RequestContextKeeper keeper) {
        return SecurityContextHolder.getContext();
    }

    @Override
    public void save(SecurityContext context, RequestContextKeeper keeper) {
        SecurityContextHolder.setContext(context);
    }

    @Override
    public void clear(SecurityContext context, RequestContextKeeper keeper) {
        SecurityContextHolder.clearContext();
    }

    @Override
    public boolean contains(SecurityContext context, RequestContextKeeper keeper) {
        return context.getAuthentication() != null;
    }

}
