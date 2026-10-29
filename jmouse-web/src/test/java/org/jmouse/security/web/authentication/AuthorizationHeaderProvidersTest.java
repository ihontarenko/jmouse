package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.jmouse.security.Authentication;
import org.jmouse.security.BadCredentialsException;
import org.jmouse.security.web.authentication.bearer.BearerTokenAuthenticationProvider;
import org.jmouse.security.web.authentication.www.BasicAuthenticationProvider;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuthorizationHeaderProvidersTest {
    @Test
    void bearerSchemeIsCaseInsensitiveAndCredentialsAreUntrusted() {
        Authentication value = new BearerTokenAuthenticationProvider().provide(request("bEaReR abc.def_123=="));
        assertEquals("abc.def_123==", value.getCredentials());
        assertFalse(value.isAuthenticated());
    }

    @Test
    void unrelatedSchemesAndMissingHeadersAreIgnored() {
        assertNull(new BearerTokenAuthenticationProvider().provide(request("BearerExtra token")));
        assertNull(new BasicAuthenticationProvider().provide(request("Digest token")));
        assertNull(new BearerTokenAuthenticationProvider().provide(request()));
    }

    @Test
    void missingAndMalformedBearerCredentialsAreRejected() {
        for (String header : List.of("Bearer", "Bearer ", "Bearer x y", "Bearer a=b", "Bearer a\tb")) {
            assertThrows(BadCredentialsException.class,
                    () -> new BearerTokenAuthenticationProvider().provide(request(header)));
        }
    }

    @Test
    void duplicateAuthorizationHeadersAreRejected() {
        assertThrows(BadCredentialsException.class,
                () -> new BearerTokenAuthenticationProvider().provide(request("Bearer one", "Bearer two")));
    }

    @Test
    void basicPreservesPasswordWhitespaceAndColons() {
        String value = Base64.getEncoder().encodeToString("user: secret: ".getBytes(StandardCharsets.ISO_8859_1));
        Authentication authentication = new BasicAuthenticationProvider().provide(request("bAsIc  " + value));
        assertEquals("user", authentication.getName());
        assertEquals(" secret: ", authentication.getCredentials());
        assertFalse(authentication.isAuthenticated());
    }

    @Test
    void malformedBasicCredentialsAreAuthenticationFailures() {
        for (String header : List.of("Basic", "Basic ?", "Basic bm9jb2xvbg==")) {
            assertThrows(BadCredentialsException.class,
                    () -> new BasicAuthenticationProvider().provide(request(header)));
        }
    }

    static HttpServletRequest request(String... headers) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getHeader")) {
                        return headers.length == 0 ? null : headers[0];
                    }
                    if (method.getName().equals("getHeaders")) {
                        return Collections.enumeration(List.of(headers));
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    return null;
                });
    }
}
