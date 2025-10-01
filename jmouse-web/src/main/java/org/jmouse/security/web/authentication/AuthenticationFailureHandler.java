package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthenticationFailureHandler {
    void onFailure(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException;
}
