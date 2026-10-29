package org.jmouse.security.web.context;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jmouse.security.SecurityContext;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.BearerTokenAuthentication;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.RequestContextKeeper;

import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class SecurityContextPersistenceFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void savingFailureCannotLeakAuthenticationToNextRequest() {
        Repository repository = new Repository();
        repository.loaded = authenticated();
        repository.saveFailure = new IllegalStateException("save failure");
        assertSame(repository.saveFailure, assertThrows(IllegalStateException.class,
                () -> filter(repository).doFilterInternal(request(), (request, response) -> { })));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void downstreamFailureRemainsPrimaryWhenSavingAlsoFails() {
        Repository repository = new Repository();
        repository.loaded = authenticated();
        repository.saveFailure = new IllegalStateException("save failure");
        ServletException original = new ServletException("handler failure");
        ServletException actual = assertThrows(ServletException.class,
                () -> filter(repository).doFilterInternal(request(), (request, response) -> { throw original; }));
        assertSame(original, actual);
        assertArrayEquals(new Throwable[]{repository.saveFailure}, actual.getSuppressed());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void clearingFailureCannotLeakAuthentication() {
        Repository repository = new Repository();
        repository.clearFailure = new IllegalStateException("clear failure");
        assertThrows(IllegalStateException.class,
                () -> filter(repository).doFilterInternal(request(), (request, response) -> { }));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void loadFailureClearsStaleThreadState() {
        SecurityContextHolder.setContext(authenticated());
        Repository repository = new Repository();
        repository.loadFailure = new IllegalStateException("load failure");
        assertThrows(IllegalStateException.class,
                () -> filter(repository).doFilterInternal(request(), (request, response) -> fail("chain must not run")));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void successiveRequestsOnOneThreadHaveIndependentContexts() throws Exception {
        Repository repository = new Repository();
        repository.loaded = authenticated();
        filter(repository).doFilterInternal(request(), (request, response) -> assertNotNull(SecurityContextHolder.getContext().getAuthentication()));
        repository.loaded = null;
        filter(repository).doFilterInternal(request(), (request, response) -> assertNull(SecurityContextHolder.getContext().getAuthentication()));
    }

    private SecurityContextPersistenceFilter filter(Repository repository) { return new SecurityContextPersistenceFilter(repository); }
    private SecurityContext authenticated() { return SecurityContext.ofAuthentication(new BearerTokenAuthentication("bearer", "unit-test")); }
    private RequestContext request() { return new RequestContext(stub(HttpServletRequest.class), stub(HttpServletResponse.class)); }
    private <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, arguments) -> {
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            return null;
        }));
    }
    private static class Repository implements SecurityContextRepository {
        SecurityContext loaded;
        RuntimeException loadFailure;
        RuntimeException saveFailure;
        RuntimeException clearFailure;
        @Override public SecurityContext load(RequestContextKeeper keeper) { if (loadFailure != null) throw loadFailure; return loaded; }
        @Override public void save(SecurityContext context, RequestContextKeeper keeper) { if (saveFailure != null) throw saveFailure; }
        @Override public void clear(SecurityContext context, RequestContextKeeper keeper) { if (clearFailure != null) throw clearFailure; }
        @Override public boolean contains(SecurityContext context, RequestContextKeeper keeper) { return loaded != null; }
    }
}
