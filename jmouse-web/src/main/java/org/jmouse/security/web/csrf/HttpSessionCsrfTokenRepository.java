package org.jmouse.security.web.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** Synchronizer tokens stored with the HTTP session; unrelated sessions cannot share a token. */
public final class HttpSessionCsrfTokenRepository implements CsrfTokenRepository {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final String headerName;
    private final String parameterName;
    private final String attributeName;
    private final int tokenBytes;

    public HttpSessionCsrfTokenRepository() {
        this("X-CSRF-Token", "_csrf", HttpSessionCsrfTokenRepository.class.getName() + ".TOKEN", 32);
    }

    public HttpSessionCsrfTokenRepository(String headerName, String parameterName, String attributeName, int tokenBytes) {
        this.headerName = Objects.requireNonNull(headerName, "headerName");
        this.parameterName = Objects.requireNonNull(parameterName, "parameterName");
        this.attributeName = Objects.requireNonNull(attributeName, "attributeName");
        if (tokenBytes < 16 || tokenBytes > 128) {
            throw new IllegalArgumentException("CSRF token length must be between 16 and 128 bytes");
        }
        this.tokenBytes = tokenBytes;
    }

    @Override
    public CsrfToken loadOrCreate(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        synchronized (session) {
            if (session.getAttribute(attributeName) instanceof CsrfToken token) {
                return token;
            }
            byte[] bytes = new byte[tokenBytes];
            RANDOM.nextBytes(bytes);
            CsrfToken token = new CsrfToken(headerName, parameterName,
                    Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
            session.setAttribute(attributeName, token);
            return token;
        }
    }

    @Override
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session) {
                session.removeAttribute(attributeName);
            }
        }
    }

    @Override
    public boolean requiresSession() {
        return true;
    }
}
