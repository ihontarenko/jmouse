package org.jmouse.security.web.authentication.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class SubmitFormRedirectAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final String loginPageUrl;

    public SubmitFormRedirectAuthenticationEntryPoint(String loginPageUrl) {
        this.loginPageUrl = loginPageUrl;
    }

    @Override
    public void initiate(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        response.sendRedirect(request.getContextPath() + loginPageUrl);
    }

}
