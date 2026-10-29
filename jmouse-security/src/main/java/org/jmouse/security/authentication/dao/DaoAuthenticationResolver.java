package org.jmouse.security.authentication.dao;

import org.jmouse.security.Authentication;
import org.jmouse.security.BadCredentialsException;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.UserPrincipalNotFoundException;
import org.jmouse.security.UserPrincipalService;
import org.jmouse.security.authentication.AbstractUsernamePasswordAuthenticationResolver;
import org.jmouse.security.authentication.AuthenticationException;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.password.PasswordEncoder;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import java.util.Arrays;
import java.util.Objects;

/** Validates enabled local users and returns an authenticated principal without password material. */
public class DaoAuthenticationResolver extends AbstractUsernamePasswordAuthenticationResolver {
    private PasswordEncoder passwordEncoder = new Pbkdf2Sha256PasswordEncoder();

    public DaoAuthenticationResolver() {
    }

    public DaoAuthenticationResolver(UserPrincipalService service, PasswordEncoder encoder) {
        setPrincipalService(Objects.requireNonNull(service, "principalService"));
        setPasswordEncoder(encoder);
    }

    public void setPasswordEncoder(PasswordEncoder encoder) {
        passwordEncoder = Objects.requireNonNull(encoder, "passwordEncoder");
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof UsernamePasswordAuthentication supplied)) {
            throw new AuthenticationException("Unsupported authentication type");
        }
        if (getPrincipalService() == null) {
            throw new IllegalStateException("UserPrincipalService must be configured for password authentication");
        }
        if (supplied.getName() == null || supplied.getName().isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        UserPrincipal principal;
        try {
            principal = getPrincipalService().loadUser(supplied.getName());
        } catch (UserPrincipalNotFoundException missing) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (principal == null || !principal.enabled()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        char[] password;
        if (supplied.getCredentials() instanceof char[] characters) {
            password = characters.clone();
        } else if (supplied.getCredentials() instanceof String characters) {
            password = characters.toCharArray();
        } else {
            throw new BadCredentialsException("Invalid credentials");
        }
        try {
            if (!passwordEncoder.verify(password, principal.password())) {
                throw new BadCredentialsException("Invalid credentials");
            }
        } finally {
            Arrays.fill(password, '\0');
        }
        UserPrincipal publicPrincipal = UserPrincipal.create(
                principal.username(), null, java.util.List.copyOf(principal.authorities()), principal.enabled());
        return new UsernamePasswordAuthentication(publicPrincipal, null, principal.authorities());
    }
}
