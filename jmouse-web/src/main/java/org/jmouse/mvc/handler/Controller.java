package org.jmouse.mvc.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface Controller {

    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
