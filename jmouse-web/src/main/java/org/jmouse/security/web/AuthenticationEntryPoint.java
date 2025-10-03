package org.jmouse.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AuthenticationEntryPoint {
    void initiate(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException;
}
