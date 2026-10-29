package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.web.authentication.www.BasicAuthenticationFilter;
import org.jmouse.security.web.context.NullSecurityContextRepository;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.match.routing.MatcherCriteria;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFilterBoundaryTest {
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedDownstreamFailureIsNotReclassifiedAsCredentialFailure() {
        checkDownstreamFailure(null);
    }

    @Test
    void authenticatedDownstreamFailureIsNotReclassifiedAsCredentialFailure() {
        checkDownstreamFailure("Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }

    private void checkDownstreamFailure(String authorization) {
        AtomicInteger failures = new AtomicInteger();
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(
                supplied -> new UsernamePasswordAuthentication(
                        UserPrincipal.create("user", null, List.of(), true), null, List.of()),
                new NullSecurityContextRepository(), MatcherCriteria.any(),
                (request, response) -> { },
                (request, response, failure) -> failures.incrementAndGet());
        filter.setContinueChainBeforeSuccess(true);
        IOException original = new IOException("application failure");
        assertSame(original, assertThrows(IOException.class, () -> filter.doFilterInternal(
                request(authorization), (request, response) -> { throw original; })));
        assertEquals(0, failures.get());
    }

    private RequestContext request(String authorization) {
        var attributes = new HashMap<String, Object>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, arguments) -> {
                    return switch (method.getName()) {
                        case "getMethod" -> "GET";
                        case "getRequestURI", "getServletPath" -> "/";
                        case "getContextPath" -> "";
                        case "getHeader" -> authorization;
                        case "getHeaderNames" -> Collections.emptyEnumeration();
                        case "getHeaders" -> Collections.enumeration(authorization == null ? List.of() : List.of(authorization));
                        case "getAttribute" -> attributes.get(arguments[0]);
                        case "setAttribute" -> attributes.put((String) arguments[0], arguments[1]);
                        case "getParameterMap" -> java.util.Map.of();
                        default -> method.getReturnType() == boolean.class ? false :
                                method.getReturnType() == int.class ? 0 : null;
                    };
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (proxy, method, arguments) -> method.getReturnType() == boolean.class ? false :
                        method.getReturnType() == int.class ? 0 : null);
        attributes.put(org.jmouse.web.http.RequestRoute.REQUEST_ROUTE_ATTRIBUTE,
                new org.jmouse.web.http.RequestRoute(org.jmouse.http.HttpMethod.GET,
                        null, null, null, null, java.util.Set.of(), request));
        return new RequestContext(request, response);
    }
}
