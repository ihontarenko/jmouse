package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class OptionsRequestHttpHandler implements RequestHttpHandler {

    private final Set<HttpMethod> allowedMethods;

    public OptionsRequestHttpHandler(Set<HttpMethod> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.NO_CONTENT.getCode());
        response.setHeader(HttpHeader.ALLOW.value(), allowedMethods.stream().map(HttpMethod::name).collect(
                Collectors.joining(", ")));
    }

}
