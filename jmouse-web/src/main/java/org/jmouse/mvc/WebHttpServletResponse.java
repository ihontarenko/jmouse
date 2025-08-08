package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.converter.HttpOutputMessage;
import org.jmouse.web.request.Headers;

import java.io.IOException;
import java.io.OutputStream;

public class WebHttpServletResponse implements HttpOutputMessage {

    private final HttpServletResponse response;
    private final Headers headers;

    public WebHttpServletResponse(HttpServletResponse response) {
        this.response = response;
        this.headers = new Headers();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

}
