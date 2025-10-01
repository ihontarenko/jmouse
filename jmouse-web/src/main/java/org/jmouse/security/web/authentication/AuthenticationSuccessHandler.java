package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthenticationSuccessHandler {
    void onSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
