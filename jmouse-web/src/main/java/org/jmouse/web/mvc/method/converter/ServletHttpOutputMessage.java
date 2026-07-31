package org.jmouse.web.mvc.method.converter;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.http.Headers;
import org.jmouse.web.http.response.HeadersBuffer;
import org.jmouse.web.http.response.HttpServletHeadersBuffer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 🌐 {@link HttpOutputMessage} implementation backed by {@link HttpServletResponse}.
 *
 * <p>Wraps a servlet response and defers header writing via {@link HeadersBuffer},
 * ensuring headers are applied only once, right before the body is written.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>✔️ Buffers headers until first body access.</li>
 *   <li>✔️ Delegates header storage and write logic to {@link HttpServletHeadersBuffer}.</li>
 *   <li>✔️ Provides direct access to the underlying {@link HttpServletResponse}.</li>
 * </ul>
 *
 * @author Ivan
 */
public class ServletHttpOutputMessage implements HttpOutputMessage {

    /**
     * 🎯 Target servlet response.
     */
    private final HttpServletResponse response;

    /**
     * 📑 Local header buffer for delayed write.
     */
    private final HeadersBuffer buffer;

    /**
     * 🏗 Create a new output message for the given servlet response.
     *
     * @param response the target servlet response
     */
    public ServletHttpOutputMessage(HttpServletResponse response) {
        this.response = response;
        this.buffer = new HttpServletHeadersBuffer();
    }

    /**
     * ✍️ Access the servlet output stream, flushing headers first if needed.
     *
     * @return servlet response output stream
     * @throws IOException if the stream cannot be opened
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        writeHeaders();
        return response.getOutputStream();
    }

    /**
     * 📑 Access the header buffer associated with this message.
     *
     * <p>Allows deferred mutation of headers before committing them to the servlet response.</p>
     *
     * @return the associated {@link HeadersBuffer}, never {@code null}
     */
    @Override
    public HeadersBuffer getHeadersBuffer() {
        return buffer;
    }

    /**
     * 📥 Access local header collection.
     *
     * @return buffered {@link Headers}, never {@code null}
     */
    @Override
    public Headers getHeaders() {
        return buffer.getHeaders();
    }

    /**
     * 📡 Write all buffered headers to the servlet response.
     *
     * <p>Idempotent: headers are only written once, further calls are ignored.</p>
     */
    public void writeHeaders() {
        buffer.write(response);
    }

    /**
     * 🔍 Check whether headers have already been written.
     *
     * @return {@code true} if headers were committed, {@code false} otherwise
     */
    public boolean isWritten() {
        return buffer.isWritten();
    }

    /**
     * 🎯 Access the underlying servlet response.
     *
     * @return the wrapped {@link HttpServletResponse}
     */
    public HttpServletResponse getResponse() {
        return response;
    }
}
