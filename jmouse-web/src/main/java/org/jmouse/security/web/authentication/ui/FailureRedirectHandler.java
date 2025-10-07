package org.jmouse.security.web.authentication.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.web.http.HttpHeader;

import java.io.IOException;

public class FailureRedirectHandler implements AuthenticationFailureHandler {


    public static final String  XML_HTTP_REQUEST = "XMLHttpRequest";
    private final       String  loginPage;
    private final       boolean use401ForAjax;

    public FailureRedirectHandler(String loginPage) {
        this(loginPage, true);
    }

    public FailureRedirectHandler(String loginPage, boolean use401ForAjax) {
        this.loginPage = loginPage;
        this.use401ForAjax = use401ForAjax;
    }

    @Override
    public void onFailure(HttpServletRequest request, HttpServletResponse response, Exception failure) throws IOException {
        if (use401ForAjax && XML_HTTP_REQUEST.equals(request.getHeader(HttpHeader.X_REQUESTED_WITH.value()))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        response.sendRedirect(request.getContextPath() + loginPage + "?e");
    }

}
