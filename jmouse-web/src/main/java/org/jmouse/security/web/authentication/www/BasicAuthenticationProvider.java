package org.jmouse.security.web.authentication.www;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authentication.BadCredentialsException;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.authentication.AbstractAuthenticationProvider;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthenticationProvider extends AbstractAuthenticationProvider {

    @Override
    protected Authentication doProvide(HttpServletRequest request) {
        Headers headers = RequestHeaders.ofRequest(request).headers();
        String  header  = (String) headers.getHeader(HttpHeader.AUTHORIZATION);

        if (header == null || !header.startsWith("Basic")) {
            return null;
        }

        String base64    = header.substring(6).trim();
        byte[] decoded   = decode(base64.getBytes(StandardCharsets.UTF_8));
        String token     = new String(decoded, StandardCharsets.ISO_8859_1);
        int    delimiter = token.indexOf(':');

        if (delimiter < 0) {
            throw new BadCredentialsException("Delimiter ':' must be followed after username.");
        }
        String username = token.substring(0, delimiter);
        String password = token.substring(delimiter + 1);

        return new UsernamePasswordAuthentication(username.trim(), password.trim());
    }

    private byte[] decode(byte[] base64Token) {
        try {
            return Base64.getDecoder().decode(base64Token);
        }
        catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid BASE64 value in 'Authorization' header");
        }
    }

}
