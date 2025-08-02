package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class WebHttpSession extends WebHttpRequest implements WebSession {

    public WebHttpSession(HttpServletRequest request) {
        super(request);
    }

    @Override
    public HttpSession getSession() {
        HttpSession session = getCurrentSession();

        if (session == null) {
            session = getRequest().getSession(true);
        }

        if (session == null) {
            throw new IllegalStateException("Unable to create session");
        }

        return session;
    }

    @Override
    public HttpSession getCurrentSession() {
        return getRequest().getSession(false);
    }

    @Override
    public String getId() {
        return getSession().getId();
    }

    @Override
    public Instant getCreationTime() {
        HttpSession session = getCurrentSession();

        if (session == null) {
            return null;
        }

        return Instant.ofEpochMilli(session.getCreationTime());
    }

    @Override
    public Instant getLastAccessedTime() {
        HttpSession session = getCurrentSession();

        if (session == null) {
            return null;
        }

        return Instant.ofEpochMilli(session.getLastAccessedTime());
    }

    @Override
    public boolean isNew() {
        return getSession().isNew();
    }

    @Override
    public void invalidate() {
        HttpSession session;
        if ((session = getCurrentSession()) != null) {
            session.invalidate();
        }
    }

    @Override
    public Object getAttribute(String name) {
        HttpSession session = getSession();

        if (session != null) {
            return session.getAttribute(name);
        }

        return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
        getSession().setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        getSession().removeAttribute(name);
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        HttpSession session = getSession();

        if (session == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> map   = new HashMap<>();
        Enumeration<String> names = session.getAttributeNames();

        while (names.hasMoreElements()) {
            String key = names.nextElement();
            map.put(key, session.getAttribute(key));
        }

        return Collections.unmodifiableMap(map);
    }

}
