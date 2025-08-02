package org.jmouse.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpHeader;

import java.util.Enumeration;

public record RequestHeaders(Headers headers) {

    public static RequestHeaders ofRequest(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        Headers             headers     = new Headers();

        while (headerNames.hasMoreElements()) {
            String     headerName = headerNames.nextElement();
            HttpHeader httpHeader = HttpHeader.ofHeader(headerName);

            if (httpHeader != null) {
                headers.addHeader(httpHeader, request.getHeader(httpHeader.value()));
            }
        }

        return new RequestHeaders(headers);
    }

}
