package org.jmouse.web.mvc.method.converter;

import org.jmouse.web.http.response.HeadersBuffer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 📤 Contract for HTTP output messages.
 *
 * <p>Extends {@link HttpMessage} to represent an HTTP request or response
 * that can expose its body as an {@link OutputStream} for writing.</p>
 *
 * <h3>Typical usage</h3>
 * <ul>
 *   <li>Used by {@link HttpMessageConverter} implementations to write
 *       serialized objects into the HTTP response body.</li>
 *   <li>Headers can be buffered via a {@link HeadersBuffer} before they are
 *       committed to the underlying servlet container.</li>
 * </ul>
 *
 * @author Ivan
 */
public interface HttpOutputMessage extends HttpMessage {

    /**
     * 🔼 Get the body output stream of the message.
     *
     * <p>Implementations must ensure that any buffered headers are written
     * into the underlying response before this stream is returned.</p>
     *
     * @return the body output stream (never {@code null})
     * @throws IOException if the stream cannot be obtained
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * 📑 Access the header buffer associated with this message.
     *
     * <p>Provides indirection so headers can be collected, mutated, and only
     * written once at the right time. This avoids premature commitment of
     * the underlying response.</p>
     *
     * @return the associated {@link HeadersBuffer}, never {@code null}
     */
    HeadersBuffer getHeadersBuffer();
}
