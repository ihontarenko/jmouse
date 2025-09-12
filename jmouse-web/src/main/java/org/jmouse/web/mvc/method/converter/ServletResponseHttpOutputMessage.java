package org.jmouse.web.mvc.method.converter;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * 🌐 Adapter that bridges {@link HttpServletResponse} with {@link HttpOutputMessage}.
 *
 * <p>Acts as a wrapper around the servlet response, buffering headers
 * until the first body write. This ensures headers can be manipulated
 * consistently before being committed.</p>
 *
 * <p>💡 Commonly used by message converters when serializing responses.</p>
 */
public class ServletResponseHttpOutputMessage implements HttpOutputMessage {

    /**
     * 🎯 Target servlet response.
     */
    private final HttpServletResponse response;

    /**
     * 📑 Local header storage before writing to servlet response.
     */
    private final Headers headers;

    /**
     * 🚦 Guard to ensure headers are written only once.
     */
    private boolean headersWritten = false;

    /**
     * 🏗️ Create a new wrapper for {@link HttpServletResponse}.
     *
     * @param response target servlet response
     */
    public ServletResponseHttpOutputMessage(HttpServletResponse response) {
        this.response = response;
        this.headers = new Headers();
    }

    /**
     * ✍️ Get the response output stream, flushing headers first if not already sent.
     *
     * @return servlet response output stream
     * @throws IOException if output stream cannot be obtained
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        writeHeaders();
        return response.getOutputStream();
    }

    /**
     * 📥 Access the buffered headers.
     *
     * @return header collection to be applied on first write
     */
    @Override
    public Headers getHeaders() {
        return headers;
    }

    /**
     * 📡 Write headers into the servlet response (only once).
     *
     * <ul>
     *   <li>Applies all custom headers.</li>
     *   <li>Resolves and sets {@code Content-Type}.</li>
     *   <li>Sets response character encoding if provided.</li>
     * </ul>
     */
    protected void writeHeaders() {
        if (!headersWritten) {
            headersWritten = true;

            getHeaders().asMap().forEach((headerName, headerValue) -> {
                response.setHeader(headerName.value(), headerValue.toString());
            });

            MediaType contentType = getHeaders().getContentType();
            Charset   charset     = contentType.getCharset();

            if (charset != null) {
                response.setCharacterEncoding(charset);
            }
        }
    }

    /**
     * 🎯 Access the underlying {@link HttpServletResponse}.
     *
     * @return raw servlet response
     */
    public HttpServletResponse getResponse() {
        return response;
    }
}
