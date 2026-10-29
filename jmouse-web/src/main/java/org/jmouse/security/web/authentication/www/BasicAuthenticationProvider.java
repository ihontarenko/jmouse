package org.jmouse.security.web.authentication.www;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.Authentication;
import org.jmouse.security.BadCredentialsException;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationProvider;
import org.jmouse.security.web.authentication.AuthorizationHeaderCredentials;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class BasicAuthenticationProvider extends AbstractAuthenticationProvider {
    private final Charset charset;

    public BasicAuthenticationProvider() {
        this(StandardCharsets.ISO_8859_1);
    }

    public BasicAuthenticationProvider(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    protected Authentication doProvide(HttpServletRequest request) {
        String value = AuthorizationHeaderCredentials.read(request, "Basic");
        if (value == null) {
            return null;
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(value), charset);
        } catch (IllegalArgumentException malformed) {
            throw new BadCredentialsException("Invalid Base64 authentication credentials");
        }
        int separator = decoded.indexOf(':');
        if (separator < 0 || decoded.chars().anyMatch(character -> character < 32 || character == 127)) {
            throw new BadCredentialsException("Malformed Basic authentication credentials");
        }
        return new UsernamePasswordAuthentication(decoded.substring(0, separator), decoded.substring(separator + 1));
    }
}
