package org.jmouse.web.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;

import java.io.IOException;

public class FrameworkDispatcherServlet extends DispatcherServlet {

    private final WebBeanContext context;

    public FrameworkDispatcherServlet(WebBeanContext context) {
        this.context = context;
    }

    @Override
    protected void doDispatch(HttpServletRequest rq, HttpServletResponse rs, HttpMethod method) throws IOException {
        rs.setStatus(HttpStatus.OK.getCode());
        rs.getWriter().write("<h1>ZAiS</h1>");
        rs.getWriter().write(method.name());
        rs.getWriter().write("<h2>" + getServletName() + "</h2>");
    }

}
