package org.jmouse.web.method.converter;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class WebHttpServletResponse implements HttpOutputMessage {

    private final HttpServletResponse response;
    private final Headers             headers;
    private       boolean             headersWritten = false;

    public WebHttpServletResponse(HttpServletResponse response) {
        this.response = response;
        this.headers = new Headers();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        writeHeaders();
        return response.getOutputStream();
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

    protected void writeHeaders() {
        if (!headersWritten) {
            headersWritten = true;

            getHeaders().asMap().forEach((headerName, headerValue) -> {
                response.setHeader(headerName.value(), headerValue.toString());
            });

            MediaType contentType = getHeaders().getContentType();
            response.setContentType(contentType.getStringType());

            Charset charset = contentType.getCharset();
            if (charset != null) {
                response.setCharacterEncoding(charset);
            }
        }
    }

}
