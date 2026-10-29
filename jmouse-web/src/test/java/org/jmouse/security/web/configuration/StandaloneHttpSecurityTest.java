package org.jmouse.security.web.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jmouse.core.Customizer;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import org.jmouse.security.service.InMemoryUserPrincipalService;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.access.ExceptionTranslationFilter;
import org.jmouse.security.web.authorization.AuthorizationFilter;
import org.jmouse.security.web.authentication.www.BasicAuthenticationFilter;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.request.RequestAttributeSecurityContextRepository;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.web.http.cache.NullRequestCache;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StandaloneHttpSecurityTest {
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildsAndAuthenticatesWithoutAWebBeanContext() throws Exception {
        HttpSecurity http = basic();
        var chain = http.build();
        AuthenticationManager manager = http.getSharedObject(AuthenticationManager.class);
        assertTrue(manager.authenticate(new UsernamePasswordAuthentication("user", "secret")).isAuthenticated());
        var filters = chain.getFilters().stream()
                .map(filter -> ((OrderedFilter) filter).filter().getClass()).toList();
        assertTrue(filters.indexOf(ExceptionTranslationFilter.class) < filters.indexOf(AuthorizationFilter.class));
        assertTrue(filters.indexOf(ExceptionTranslationFilter.class) < filters.indexOf(BasicAuthenticationFilter.class));
    }

    @Test
    void statelessPolicySelectsRequestLocalStorageAndNoSessionRequestCache() throws Exception {
        HttpSecurity http = basic();
        http.build();
        assertInstanceOf(RequestAttributeSecurityContextRepository.class,
                http.getSharedObject(SecurityContextRepository.class));
        assertInstanceOf(NullRequestCache.class, http.getSharedObject(SharedAttributes.REQUEST_CACHE));
    }

    @Test
    void missingPasswordResolverFailsAtBuildTime() {
        HttpSecurity http = new HttpSecurity();
        http.authentication(authentication -> authentication.httpBasic(Customizer.noop()));
        assertThrows(IllegalStateException.class, http::build);
    }

    private HttpSecurity basic() {
        Pbkdf2Sha256PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder(1000, 16, 32);
        UserPrincipal user = UserPrincipal.User.builder().username("user")
                .password(encoder.encode("secret".toCharArray())).authority("ROLE_USER").build();
        HttpSecurity http = new HttpSecurity();
        http.principalService(new InMemoryUserPrincipalService(encoder, user)).passwordEncoder(encoder);
        http.csrf(csrf -> csrf.disable());
        http.securityContext(Customizer.noop());
        http.sessionManagement(session -> session.policy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(Customizer.noop());
        http.authentication(authentication -> authentication.httpBasic(Customizer.noop()));
        http.authorization(authorize -> authorize.anyRequest().authenticated());
        return http;
    }
}
