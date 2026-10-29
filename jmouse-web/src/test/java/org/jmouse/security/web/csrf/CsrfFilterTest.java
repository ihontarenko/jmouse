package org.jmouse.security.web.csrf;

import org.junit.jupiter.api.Test;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.web.http.RequestContextKeeper;
import org.jmouse.web.testing.ServletExchange;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CsrfFilterTest {
    private final HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
    private final CsrfFilter filter = new CsrfFilter(repository, SessionCreationPolicy.IF_REQUIRED);

    @Test
    void safeRequestPublishesASessionToken() throws Exception {
        ServletExchange exchange = seeded();
        CsrfToken token = (CsrfToken) exchange.attributes.get(CsrfToken.REQUEST_ATTRIBUTE);
        assertNotNull(token);
        assertEquals(token.value(), exchange.responseHeaders.get(token.headerName()));
        assertEquals(1, exchange.sessionCreations);
        assertEquals("no-store", exchange.responseHeaders.get("Cache-Control"));
    }

    @Test
    void mutationWithoutTokenIsForbidden() throws Exception {
        ServletExchange exchange = seeded();
        exchange.method = "POST";
        AtomicInteger reached = new AtomicInteger();
        filter.doFilterInternal(exchange.context(), (request, response) -> reached.incrementAndGet());
        assertEquals(403, exchange.status);
        assertEquals(0, reached.get());
    }

    @Test
    void exactHeaderTokenAllowsMutation() throws Exception {
        ServletExchange exchange = seeded();
        CsrfToken token = token(exchange);
        exchange.method = "POST";
        exchange.headers.put(token.headerName(), List.of(token.value()));
        AtomicInteger reached = new AtomicInteger();
        filter.doFilterInternal(exchange.context(), (request, response) -> reached.incrementAndGet());
        assertEquals(1, reached.get());
    }

    @Test
    void formParameterTokenAllowsMutation() throws Exception {
        ServletExchange exchange = seeded();
        CsrfToken token = token(exchange);
        exchange.method = "POST";
        exchange.parameters.put(token.parameterName(), new String[]{token.value()});
        AtomicInteger reached = new AtomicInteger();
        filter.doFilterInternal(exchange.context(), (request, response) -> reached.incrementAndGet());
        assertEquals(1, reached.get());
    }

    @Test
    void anotherSessionsTokenIsRejected() throws Exception {
        ServletExchange exchange = seeded();
        CsrfToken other = token(seeded());
        exchange.method = "POST";
        exchange.headers.put(other.headerName(), List.of(other.value()));
        filter.doFilterInternal(exchange.context(), (request, response) -> fail("must not run"));
        assertEquals(403, exchange.status);
    }

    @Test
    void duplicateHeaderTokensAreRejected() throws Exception {
        ServletExchange exchange = seeded();
        CsrfToken token = token(exchange);
        exchange.method = "POST";
        exchange.headers.put(token.headerName(), List.of(token.value(), token.value()));
        filter.doFilterInternal(exchange.context(), (request, response) -> fail("must not run"));
        assertEquals(403, exchange.status);
    }

    @Test
    void existingSessionOnlyPolicyDoesNotAllocateASession() throws Exception {
        ServletExchange exchange = new ServletExchange();
        CsrfFilter existingOnly = new CsrfFilter(repository, SessionCreationPolicy.IF_PRESENT);
        existingOnly.doFilterInternal(exchange.context(), (request, response) -> { });
        assertEquals(0, exchange.sessionCreations);
        exchange.method = "POST";
        existingOnly.doFilterInternal(exchange.context(), (request, response) -> fail("must not run"));
        assertEquals(403, exchange.status);
        assertEquals(0, exchange.sessionCreations);
    }

    @Test
    void sessionRepositoryCannotMasqueradeAsStateless() {
        assertThrows(IllegalStateException.class,
                () -> new CsrfFilter(repository, SessionCreationPolicy.STATELESS));
    }

    @Test
    void authenticationRotationInvalidatesThePreviousToken() throws Exception {
        ServletExchange exchange = seeded();
        CsrfToken old = token(exchange);
        new CsrfAuthenticationStrategy(repository, true).onAuthentication(null,
                RequestContextKeeper.ofRequestContext(exchange.context()));
        CsrfToken current = token(exchange);
        assertNotEquals(old.value(), current.value());
        exchange.method = "POST";
        exchange.headers.put(old.headerName(), List.of(old.value()));
        filter.doFilterInternal(exchange.context(), (request, response) -> fail("old token must not run"));
        assertEquals(403, exchange.status);
    }

    private ServletExchange seeded() throws Exception {
        ServletExchange exchange = new ServletExchange();
        filter.doFilterInternal(exchange.context(), (request, response) -> { });
        return exchange;
    }

    private CsrfToken token(ServletExchange exchange) {
        return (CsrfToken) exchange.attributes.get(CsrfToken.REQUEST_ATTRIBUTE);
    }
}
