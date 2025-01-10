package svit.web.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import svit.web.http.HttpStatus;

import java.io.IOException;

public class FrameworkDispatcherServlet extends DispatcherServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                                                                                            IOException {
        ServletContext servletContext = getServletContext();

        response.setStatus(HttpStatus.OK.getCode());
        response.getWriter().write(getClass().getName());
    }

}
