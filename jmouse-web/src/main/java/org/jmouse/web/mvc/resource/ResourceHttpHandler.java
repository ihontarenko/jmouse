package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

import java.io.IOException;

public class ResourceHttpHandler implements RequestHttpHandler {

    /**
     * Handles the incoming request and writes the response.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @throws IOException in case of I/O errors
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

    }

}
