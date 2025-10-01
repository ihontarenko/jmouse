package org.jmouse.security.web.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.core.SecurityContext;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.http.request.RequestContextKeeper;
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
        HttpServletRequest  request  = requestContext.request();
        HttpServletResponse response = requestContext.response();

        RequestContextKeeper keeper = RequestContextKeeper.ofRequestContext(requestContext);

        keeper.toRequestContext();

        SecurityContext     current  = repository.load(requestContext);

        SecurityContextHolder.setContext(current != null ? current : SecurityContext.empty());

        try {
            RequestContext newRequestContext = keeper.toRequestContext();
            chain.doFilter(newRequestContext.request(), newRequestContext.response());
        } finally {
            SecurityContext after = SecurityContextHolder.getContext();

            if (after != null && after.getAuthentication() != null) {
                repository.save(after);
            } else {
                repository.clear(after);
            }

            SecurityContextHolder.clearContext();
        }

    }

}
