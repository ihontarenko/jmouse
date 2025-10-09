package org.jmouse.security.web.authentication.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public final class LoginUrlAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final String loginPage;

    public LoginUrlAuthenticationEntryPoint(String loginPage) {
        this.loginPage = loginPage;
    }

    @Override
    public void initiate(HttpServletRequest request, HttpServletResponse response, Exception e) throws IOException {
        if (!response.isCommitted()) {
            response.sendRedirect(loginPage + "?_=" + getClass().getSimpleName());
        }
    }

}
