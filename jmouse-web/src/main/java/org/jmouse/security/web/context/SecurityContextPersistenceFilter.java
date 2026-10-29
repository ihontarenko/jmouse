package org.jmouse.security.web.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.jmouse.security.SecurityContext;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.web.session.SessionPersistenceResponseWrapper;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.RequestContextKeeper;
import org.jmouse.web.servlet.filter.BeanFilter;

import java.io.IOException;

/** Loads request authentication and always clears thread state, including repository failures. */
public class SecurityContextPersistenceFilter implements BeanFilter {

    private final SecurityContextRepository repository;

    public SecurityContextPersistenceFilter(SecurityContextRepository repository) {
        this.repository = repository;
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain)
            throws IOException, ServletException {
        RequestContextKeeper keeper = RequestContextKeeper.ofRequestContext(requestContext);
        boolean loaded = false;
        Throwable failure = null;

        try {
            SecurityContext current = repository.load(keeper);
            SecurityContextHolder.setContext(current != null ? current : SecurityContext.empty());
            loaded = true;
            chain.doFilter(keeper.request(), new SessionPersistenceResponseWrapper(repository, keeper));
        } catch (IOException | ServletException | RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            try {
                if (loaded) {
                    SecurityContext current = SecurityContextHolder.getContext();
                    if (current != null && current.getAuthentication() != null) {
                        repository.save(current, keeper);
                    } else {
                        repository.clear(current, keeper);
                    }
                }
            } catch (RuntimeException | Error persistenceFailure) {
                if (failure == null) {
                    throw persistenceFailure;
                }
                if (failure != persistenceFailure) {
                    failure.addSuppressed(persistenceFailure);
                }
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
