package org.jmouse.web.http.converter;

import java.io.IOException;
import java.io.InputStream;

/**
 * 📥 Represents an HTTP request or response body for reading.
 *
 * <p>Extends {@link HttpMessage} with access to the raw
 * {@link InputStream} of the message body.</p>
 *
 * <p>Typically used by {@link HttpMessageConverter} to read
 * and deserialize request/response content.</p>
 *
 * @author Ivan
 */
public interface HttpInputMessage extends HttpMessage {
    /**
     * 🔽 Returns the body as an input stream.
     *
     * @return message body stream (never {@code null})
     * @throws IOException if an I/O error occurs
     */
    InputStream getInputStream() throws IOException;
}
