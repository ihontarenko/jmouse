package org.jmouse.security.web.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.web.authentication.www.BasicAuthenticationFilter;
import org.jmouse.security.web.context.NullSecurityContextRepository;
import org.jmouse.web.http.RequestContextKeeper;
import org.jmouse.web.match.routing.MatcherCriteria;
import org.jmouse.web.testing.ServletExchange;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SessionAuthenticationStrategyTest {
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sessionIdChangesBeforePersistenceAndSuccessResponse() throws Exception {
        ServletExchange exchange = exchange();
        AtomicInteger success = new AtomicInteger();
        NullSecurityContextRepository repository = new NullSecurityContextRepository() {
            @Override
            public void save(org.jmouse.security.SecurityContext context, RequestContextKeeper keeper) {
                assertEquals(1, exchange.rotations);
            }
        };
        BasicAuthenticationFilter filter = filter(repository, (request, response) -> {
            assertEquals(1, exchange.rotations);
            success.incrementAndGet();
        });
        filter.setSessionAuthenticationHandler(new ChangeSessionIdAuthenticateHandler());
        filter.doFilterInternal(exchange.context(), (request, response) -> fail("login response terminates chain"));
        assertEquals(1, success.get());
    }

    @Test
    void repeatedSameIdentityDoesNotRotateSessionOnEveryRequest() throws Exception {
        ServletExchange exchange = exchange();
        SecurityContextHolder.setContext(org.jmouse.security.SecurityContext.ofAuthentication(authenticated()));
        BasicAuthenticationFilter filter = filter(new NullSecurityContextRepository(), (request, response) -> { });
        filter.setSessionAuthenticationHandler(new ChangeSessionIdAuthenticateHandler());
        filter.doFilterInternal(exchange.context(), (request, response) -> { });
        assertEquals(0, exchange.rotations);
    }

    @Test
    void failedSessionStrategyDoesNotPersistAuthentication() {
        ServletExchange exchange = exchange();
        BasicAuthenticationFilter filter = filter(new NullSecurityContextRepository(), (request, response) -> fail("must not run"));
        IllegalStateException original = new IllegalStateException("session policy failure");
        filter.setSessionAuthenticationHandler((authentication, keeper) -> { throw original; });
        assertSame(original, assertThrows(IllegalStateException.class,
                () -> filter.doFilterInternal(exchange.context(), (request, response) -> fail("must not run"))));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private BasicAuthenticationFilter filter(NullSecurityContextRepository repository,
                                             org.jmouse.security.web.authentication.AuthenticationSuccessHandler success) {
        return new BasicAuthenticationFilter(supplied -> authenticated(), repository, MatcherCriteria.any(),
                success, (request, response, failure) -> fail(failure));
    }

    private UsernamePasswordAuthentication authenticated() {
        return new UsernamePasswordAuthentication(UserPrincipal.create("user", null, List.of(), true),
                null, List.of());
    }

    private ServletExchange exchange() {
        ServletExchange exchange = new ServletExchange();
        exchange.request.getSession(true);
        exchange.headers.put("Authorization", List.of("Basic " + Base64.getEncoder().encodeToString(
                "user:secret".getBytes(java.nio.charset.StandardCharsets.US_ASCII))));
        return exchange;
    }
}
