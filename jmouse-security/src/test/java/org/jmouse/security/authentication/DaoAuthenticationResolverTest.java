package org.jmouse.security.authentication;

import org.junit.jupiter.api.Test;
import org.jmouse.security.*;
import org.jmouse.security.authentication.dao.DaoAuthenticationResolver;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import org.jmouse.security.service.InMemoryUserPrincipalService;
import static org.junit.jupiter.api.Assertions.*;

class DaoAuthenticationResolverTest {
    private final Pbkdf2Sha256PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder(1000, 16, 32);

    @Test
    void submittedCredentialsAreUntrustedAndRetainTheirUsername() {
        Authentication request = new UsernamePasswordAuthentication("ivan", " secret ");
        assertFalse(request.isAuthenticated());
        assertEquals("ivan", request.getName());
    }

    @Test
    void authenticatesOnlyAfterPasswordVerificationAndErasesReturnedSecrets() {
        InMemoryUserPrincipalService users = users(true);
        Authentication result = new DaoAuthenticationResolver(users, encoder)
                .authenticate(new UsernamePasswordAuthentication("ivan", " secret "));
        assertTrue(result.isAuthenticated());
        assertEquals("ivan", result.getName());
        assertNull(result.getCredentials());
        assertNull(((UserPrincipal) result.getPrincipal()).password());
    }

    @Test
    void rejectsWrongPasswordAndDisabledUsers() {
        assertThrows(BadCredentialsException.class, () -> new DaoAuthenticationResolver(users(true), encoder)
                .authenticate(new UsernamePasswordAuthentication("ivan", "wrong")));
        assertThrows(BadCredentialsException.class, () -> new DaoAuthenticationResolver(users(false), encoder)
                .authenticate(new UsernamePasswordAuthentication("ivan", " secret ")));
    }

    @Test
    void missingAndInvalidPasswordsHaveTheSamePublicFailure() {
        DaoAuthenticationResolver resolver = new DaoAuthenticationResolver(users(true), encoder);
        BadCredentialsException missing = assertThrows(BadCredentialsException.class,
                () -> resolver.authenticate(new UsernamePasswordAuthentication("missing", "wrong")));
        BadCredentialsException invalid = assertThrows(BadCredentialsException.class,
                () -> resolver.authenticate(new UsernamePasswordAuthentication("ivan", "wrong")));
        assertEquals(missing.getMessage(), invalid.getMessage());
    }

    @Test
    void changesStoredPasswordsUsingTheEncoder() {
        InMemoryUserPrincipalService users = users(true);
        users.changePassword(users.loadUser("ivan"), " secret ", "new value");
        assertTrue(encoder.verify("new value".toCharArray(), users.loadUser("ivan").password()));
        assertFalse(encoder.verify(" secret ".toCharArray(), users.loadUser("ivan").password()));
        assertNotEquals("new value", users.loadUser("ivan").password());
    }

    private InMemoryUserPrincipalService users(boolean enabled) {
        UserPrincipal user = UserPrincipal.User.builder().username("ivan")
                .password(encoder.encode(" secret ".toCharArray())).enabled(enabled).authority("ROLE_USER").build();
        return new InMemoryUserPrincipalService(encoder, user);
    }
}
