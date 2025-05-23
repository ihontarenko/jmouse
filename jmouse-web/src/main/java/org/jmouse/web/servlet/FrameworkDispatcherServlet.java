package org.jmouse.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpStatus;

import java.io.IOException;

public class FrameworkDispatcherServlet extends DispatcherServlet {

    private final WebBeanContext context;

    public FrameworkDispatcherServlet(WebBeanContext context) {
        this.context = context;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK.getCode());
        response.getWriter().write("<h1>ZAiS</h1>");
        response.getWriter().write("<h2>" + getServletName() + "</h2>");
    }

}
