package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.BadCredentialsException;
import java.util.Enumeration;

/** Parses the scheme boundary without trimming or rewriting secret credential bytes. */
public final class AuthorizationHeaderCredentials {
    private AuthorizationHeaderCredentials() {
    }

    public static String read(HttpServletRequest request, String expectedScheme) {
        Enumeration<String> values = request.getHeaders("Authorization");
        if (values != null && values.hasMoreElements()) {
            values.nextElement();
            if (values.hasMoreElements()) {
                throw new BadCredentialsException("Multiple Authorization headers are not allowed");
            }
        }
        String header = request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        int separator = header.indexOf(' ');
        String scheme = separator < 0 ? header : header.substring(0, separator);
        if (!expectedScheme.equalsIgnoreCase(scheme)) {
            return null;
        }
        int start = separator < 0 ? header.length() : separator;
        while (start < header.length() && header.charAt(start) == ' ') {
            start++;
        }
        if (start == header.length()) {
            throw new BadCredentialsException("Missing authentication credentials");
        }
        String credentials = header.substring(start);
        if (credentials.chars().anyMatch(character -> Character.isWhitespace(character) || Character.isISOControl(character))) {
            throw new BadCredentialsException("Malformed authentication credentials");
        }
        return credentials;
    }
}
