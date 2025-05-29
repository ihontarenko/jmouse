package org.jmouse.web.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;

import java.io.IOException;

abstract public class DispatcherServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doDispatch(request, response, HttpMethod.valueOf(request.getMethod()));
    }

    abstract protected void doDispatch(HttpServletRequest rq, HttpServletResponse rs, HttpMethod method) throws
                                                                                                         IOException;

}
