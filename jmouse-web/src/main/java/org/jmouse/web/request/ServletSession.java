package org.jmouse.web.request;

import jakarta.servlet.http.HttpSession;

public interface ServletSession extends RequestAttributes, Session {

    String SERVLET_SESSION_ATTRIBUTE = ServletSession.class.getName() + ".SERVLET_SESSION";

    HttpSession getSession();

    HttpSession getCurrentSession();
    
}
