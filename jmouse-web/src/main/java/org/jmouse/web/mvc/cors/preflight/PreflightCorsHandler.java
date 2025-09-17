package org.jmouse.web.mvc.cors.preflight;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

import java.io.IOException;

public class PreflightCorsHandler implements RequestHttpHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

    }

}
