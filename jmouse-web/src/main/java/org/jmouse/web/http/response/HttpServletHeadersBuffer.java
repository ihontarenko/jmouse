package org.jmouse.web.http.response;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 📨 Buffer for HTTP headers before committing them to a {@link HttpServletResponse}.
 *
 * <p>This implementation collects headers in a local {@link Headers} store
 * until {@link #write(HttpServletResponse)} is called. At that point, all
 * headers are applied exactly once to the target servlet response.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>✔️ Supports single and multi-value headers (multi-values are applied using {@code setHeader} + {@code addHeader}).</li>
 *   <li>✔️ Automatically resolves and applies {@code Content-Type} and its {@link Charset} if available.</li>
 *   <li>✔️ Prevents duplicate writes via {@link #isWritten()} flag.</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * HttpServletHeadersBuffer buffer = new HttpServletHeadersBuffer();
 * buffer.getHeaders().setHeader(HttpHeader.CONTENT_TYPE, "application/json");
 * buffer.write(response); // applies headers once
 * }</pre>
 *
 * <p><b>⚠️ Thread-safety:</b> Instances are not thread-safe and must be used per-request only.</p>
 *
 * @author Ivan
 */
public class HttpServletHeadersBuffer implements HeadersBuffer {

    /**
     * 🗂 Local buffer of headers before writing to servlet response.
     */
    private final Headers headers;

    /**
     * ✅ Flag indicating whether headers were already written.
     */
    private boolean written = false;

    /**
     * 🏗 Create a new buffer with an empty {@link Headers} instance.
     */
    public HttpServletHeadersBuffer() {
        this(new Headers());
    }

    /**
     * 🏗 Create a new buffer with an existing {@link Headers} instance.
     *
     * @param headers initial headers collection (must not be {@code null})
     */
    public HttpServletHeadersBuffer(Headers headers) {
        this.headers = headers;
    }

    /**
     * ✍️ Write all buffered headers into the given servlet response.
     *
     * <p>Headers are written only once. Subsequent calls are ignored.</p>
     *
     * @param response the target servlet response
     */
    @Override
    public void write(HttpServletResponse response) {
        if (!isWritten()) {
            written = true;

            headers.asMap().forEach((headerName, headerValue) -> {
                if (headerValue instanceof List<?> collection) {
                    boolean firstValue = true;
                    for (Object value : collection) {
                        if (firstValue) {
                            response.setHeader(headerName.value(), value.toString());
                            firstValue = false;
                        } else {
                            response.addHeader(headerName.value(), value.toString());
                        }
                    }
                } else {
                    response.setHeader(headerName.value(), headerValue.toString());
                }
            });

            MediaType contentType = headers.getContentType();
            if (contentType != null) {
                response.setContentType(contentType.toString());
                Charset charset = contentType.getCharset();
                if (charset != null) {
                    response.setCharacterEncoding(charset.name());
                }
            }
        }
    }

    /**
     * 📥 Access the local headers buffer.
     *
     * @return buffered {@link Headers} (never {@code null})
     */
    @Override
    public Headers getHeaders() {
        return headers;
    }

    /**
     * 🔍 Check whether headers were already written.
     *
     * @return {@code true} if headers have been committed, {@code false} otherwise
     */
    @Override
    public boolean isWritten() {
        return written;
    }
}
