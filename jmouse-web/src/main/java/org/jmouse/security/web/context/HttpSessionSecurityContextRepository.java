package org.jmouse.security.web.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationInspector;
import org.jmouse.security.core.*;
import org.jmouse.web.http.request.RequestContextKeeper;
import org.jmouse.web.http.request.WebHttpSession;

import java.util.Objects;

import static org.jmouse.beans.BeanScope.SESSION;
import static org.jmouse.web.http.request.RequestAttributes.ofRequest;

public class HttpSessionSecurityContextRepository implements SecurityContextRepository, AuthenticationInspector {

    public static final String DEFAULT_SESSION_KEY = HttpSessionSecurityContextRepository.class.getName()
            .concat(".JMOUSE_SECURITY_CONTEXT");

    private final SecurityContext               emptyPrototype        = SecurityContext.empty();
    private       String                        sessionKey            = DEFAULT_SESSION_KEY;
    private       boolean                       allowSessionCreation  = true;

    public HttpSessionSecurityContextRepository sessionKey(String key) {
        this.sessionKey = (key != null && !key.isBlank()) ? key : DEFAULT_SESSION_KEY;
        return this;
    }

    public HttpSessionSecurityContextRepository allowSessionCreation(boolean allow) {
        this.allowSessionCreation = allow;
        return this;
    }

    public HttpSessionSecurityContextRepository strategy(SecurityContextHolderStrategy strategy) {
        if (strategy != null) {
            SecurityContextHolder.setContextHolderStrategy(strategy);
        }
        return this;
    }

    @Override
    public SecurityContext load(RequestContextKeeper keeper) {
        HttpServletRequest request = keeper.request();
        HttpSession        session = request.getSession(false);
        SecurityContext    context = readFromSession(session);

        if (context == null) {
            context = SecurityContextHolder.newContext();
        }

        return context;
    }

    @Override
    public void save(SecurityContext context, RequestContextKeeper keeper) {
        if (!(ofRequest(SESSION, keeper.request(), allowSessionCreation) instanceof WebHttpSession httpSession)) {
            return;
        }

        HttpSession existing = httpSession.getCurrentSession();

        if (isEmpty(context) || (context.getAuthentication() != null && isAnonymous(context.getAuthentication()))) {
            if (existing != null) {
                existing.removeAttribute(sessionKey);
            }
            return;
        }

        HttpSession session = httpSession.getSession();
        if (session != null) {
            session.setAttribute(sessionKey, context);
        }
    }

    @Override
    public void clear(SecurityContext context, RequestContextKeeper keeper) {
        HttpServletRequest request = keeper.request();
        HttpSession        session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(sessionKey);
        }
    }

    @Override
    public boolean contains(SecurityContext context, RequestContextKeeper keeper) {
        HttpServletRequest request = keeper.request();
        HttpSession        session = request.getSession(false);
        return session != null && session.getAttribute(sessionKey) instanceof SecurityContext;
    }

    /** Helpers */

    private SecurityContext readFromSession(HttpSession session) {
        SecurityContext context = null;

        if (session != null && session.getAttribute(sessionKey) instanceof SecurityContext securityContext) {
            context = securityContext;
        }

        return context;
    }

    private boolean isEmpty(SecurityContext context) {
        if (context == null || context.getAuthentication() == null) {
            return true;
        }
        return Objects.equals(emptyPrototype, context);
    }
}
