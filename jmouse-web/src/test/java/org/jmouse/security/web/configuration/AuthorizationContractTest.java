package org.jmouse.security.web.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.security.web.configuration.configurer.AuthorizeRequestConfigurer;
import org.jmouse.web.testing.ServletExchange;
import org.junit.jupiter.api.Test;

class AuthorizationContractTest {

    @Test
    void negationIsAppliedOnce() {
        HttpSecurity http = new HttpSecurity();
        AuthorizeRequestConfigurer<HttpSecurity> rules = http.attach(new AuthorizeRequestConfigurer<>());
        rules.getRequestMatchCriterion().anyRequest().not().hasRole("ADMIN");
        var manager = rules.getRequestMatchCriterion().createAuthorizationManager();
        assertTrue(manager.check(user("ROLE_USER"), new ServletExchange().request).isGranted());
        assertFalse(manager.check(user("ROLE_ADMIN"), new ServletExchange().request).isGranted());
    }

    @Test
    void configuredRolePrefixReachesTheRolePolicy() {
        HttpSecurity http = new HttpSecurity();
        AuthorizeRequestConfigurer<HttpSecurity> rules = http.attach(new AuthorizeRequestConfigurer<>());
        rules.getRequestMatchCriterion().rolePrefix("APP_").anyRequest().hasRole("EDITOR");
        var manager = rules.getRequestMatchCriterion().createAuthorizationManager();
        assertTrue(manager.check(user("APP_EDITOR"), new ServletExchange().request).isGranted());
        assertFalse(manager.check(user("ROLE_EDITOR"), new ServletExchange().request).isGranted());
    }

    @Test
    void missingAuthenticationIsDeniedInsteadOfThrowing() {
        var manager = org.jmouse.security.authorization.AuthorityPolicyAuthorizationManager.hasRole("ADMIN");
        assertFalse(manager.check(null, null).isGranted());
        assertFalse(manager.check(new UsernamePasswordAuthentication("user", "password"), null).isGranted());
    }

    private UsernamePasswordAuthentication user(String authority) {
        var principal = UserPrincipal.User.builder().username("user").authority(authority).build();
        return new UsernamePasswordAuthentication(principal, null, principal.authorities());
    }
}
