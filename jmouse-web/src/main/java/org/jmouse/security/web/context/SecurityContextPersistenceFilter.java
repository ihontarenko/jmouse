package org.jmouse.security.web.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.core.SecurityContext;
import org.jmouse.security.web.session.SessionPersistenceResponseWrapper;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.RequestContextKeeper;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

public class SecurityContextPersistenceFilter implements BeanFilter {

    private final SecurityContextRepository repository;

    public SecurityContextPersistenceFilter(SecurityContextRepository repository) {
        this.repository = repository;
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        RequestContextKeeper       keeper  = RequestContextKeeper.ofRequestContext(requestContext);
        SecurityContext            current = repository.load(keeper);
        HttpServletResponseWrapper wrapper = new SessionPersistenceResponseWrapper(repository, keeper);

        SecurityContextHolder.setContext(current != null ? current : SecurityContext.empty());

        try {
            RequestContext newRequestContext = keeper.toRequestContext();
            chain.doFilter(newRequestContext.request(), wrapper);
        } finally {
            SecurityContext contextAfter = SecurityContextHolder.getContext();

            if (contextAfter != null && contextAfter.getAuthentication() != null) {
                repository.save(contextAfter, keeper);
            } else {
                repository.clear(contextAfter, keeper);
            }

            SecurityContextHolder.clearContext();
        }

    }

}
