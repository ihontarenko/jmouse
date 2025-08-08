package org.jmouse.mvc.converter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 📤 Represents an HTTP request or response body for writing.
 *
 * <p>Extends {@link HttpMessage} with access to the raw
 * {@link OutputStream} of the message body.</p>
 *
 * <p>Typically used by {@link HttpMessageConverter} to write
 * and serialize response/request content.</p>
 *
 * @author Ivan
 */
public interface HttpOutputMessage extends HttpMessage {

    /**
     * 🔼 Returns the body as an output stream.
     *
     * @return message body stream (never {@code null})
     * @throws IOException if an I/O error occurs
     */
    OutputStream getOutputStream() throws IOException;
}
