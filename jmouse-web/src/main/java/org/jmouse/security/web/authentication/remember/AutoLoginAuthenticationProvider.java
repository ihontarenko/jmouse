package org.jmouse.security.web.authentication.remember;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.Authentication;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.web.authentication.AuthenticationProvider;

public class AutoLoginAuthenticationProvider implements AuthenticationProvider {

    private String cookieName = "AUTO_LOGIN";

    @Override
    public Authentication provide(HttpServletRequest request) {
        Cookie cookie = findCookie(request, cookieName);

        if (cookie == null) {
            return null;
        }

        String value = cookie.getValue();

        if (value == null || value.isBlank()) {
            return null;
        }

        AutoLoginToken token = parse(value);

        if (token == null) {
            return null;
        }

        return new UsernamePasswordAuthentication(token.username(), token.token());
    }

    protected AutoLoginToken parse(String value) {
        String[] parts = value.split(":", 2);

        if (parts.length != 2) {
            return null;
        }

        String username = parts[0].trim();
        String token    = parts[1].trim();

        if (username.isEmpty() || token.isEmpty()) {
            return null;
        }

        return new AutoLoginToken(username, token);
    }

    protected Cookie findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    protected record AutoLoginToken(String username, String token) {
    }

}