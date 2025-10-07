package org.jmouse.security.web.authentication.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class SuccessRedirectHandler implements AuthenticationSuccessHandler {

    private final String target;

    public SuccessRedirectHandler(String target) {
        this.target = target;
    }

    @Override
    public void onSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + target);
    }
}