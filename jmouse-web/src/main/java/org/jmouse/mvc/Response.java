package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Represents a response that can be written to the servlet response stream.
 *
 * Allows JSON, HTML, redirect, or custom output.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface Response {
    void writeTo(HttpServletResponse response) throws IOException;
}