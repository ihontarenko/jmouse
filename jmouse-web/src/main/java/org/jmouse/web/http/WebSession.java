package org.jmouse.web.http;

import jakarta.servlet.http.HttpSession;
import org.jmouse.http.Session;

public interface WebSession extends RequestAttributes, Session {

    String SERVLET_SESSION_ATTRIBUTE = WebSession.class.getName() + ".SERVLET_SESSION";

    HttpSession getSession();

    HttpSession getCurrentSession();
    
}
