package org.jmouse.web.http.request;

import jakarta.servlet.http.HttpSession;

public interface WebSession extends RequestAttributes, Session {

    String SERVLET_SESSION_ATTRIBUTE = WebSession.class.getName() + ".SERVLET_SESSION";

    HttpSession getSession();

    HttpSession getCurrentSession();
    
}
