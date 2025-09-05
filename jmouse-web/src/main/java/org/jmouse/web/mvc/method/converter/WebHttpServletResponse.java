package org.jmouse.web.mvc.method.converter;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * 🌐 Adapter that bridges {@link HttpServletResponse} with
 * {@link HttpOutputMessage}.
 *
 * <p>Buffers headers until first write, then flushes them
 * into the underlying servlet response.</p>
 */
public class WebHttpServletResponse implements HttpOutputMessage {

    /**
     * 🎯 Target servlet response.
     */
    private final HttpServletResponse response;

    /**
     * 📑 Local header storage before writing to servlet response.
     */
    private final Headers headers;

    /**
     * 🚦 Ensures headers are written only once.
     */
    private boolean headersWritten = false;

    /**
     * 🏗️ Create wrapper for {@link HttpServletResponse}.
     *
     * @param response target servlet response
     */
    public WebHttpServletResponse(HttpServletResponse response) {
        this.response = response;
        this.headers = new Headers();
    }

    /**
     * ✍️ Get output stream, flushing headers first if not already sent.
     *
     * @return servlet response output stream
     * @throws IOException if output stream cannot be accessed
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        writeHeaders();
        return response.getOutputStream();
    }

    /**
     * 📥 Access local header collection.
     *
     * @return headers to be written before body
     */
    @Override
    public Headers getHeaders() {
        return headers;
    }

    /**
     * 📡 Write headers into servlet response (only once).
     *
     * <ul>
     *   <li>Applies all custom headers.</li>
     *   <li>Resolves {@code Content-Type} and charset.</li>
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

}
