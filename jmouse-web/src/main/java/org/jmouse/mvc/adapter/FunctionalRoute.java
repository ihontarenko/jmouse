package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@FunctionalInterface
public interface FunctionalRoute {
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
