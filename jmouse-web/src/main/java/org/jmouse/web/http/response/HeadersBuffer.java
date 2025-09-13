package org.jmouse.web.http.response;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.request.Headers;

/**
 * 📑 Contract for buffering and writing HTTP headers into a {@link HttpServletResponse}.
 *
 * <p>Implementations allow headers to be accumulated locally before
 * committing them to the underlying servlet response. This enables
 * delayed header evaluation until the first body write or explicit flush.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>🛠️ Provide access to the buffered {@link Headers} collection.</li>
 *   <li>📡 Write headers into the servlet response.</li>
 *   <li>🔍 Track whether headers have already been written.</li>
 *   <li>🧹 Optionally cleanup/reset headers if the response is not committed.</li>
 * </ul>
 */
public interface HeadersBuffer {

    /**
     * 🧹 Clear buffered headers if the servlet response has not yet been committed.
     *
     * <p>This can be useful in error-handling scenarios (e.g. when generating
     * a {@code 416 Range Not Satisfiable} response) to discard previously set
     * headers before re-applying a clean set.</p>
     *
     * <p>⚠️ Note: if the response is already committed, headers cannot be
     * cleared or rewritten.</p>
     *
     * @param response the target servlet response
     * @return {@code true} if headers were cleared; {@code false} otherwise
     */
    default boolean cleanup(HttpServletResponse response) {
        boolean cleared = false;

        if (!response.isCommitted()) {
            getHeaders().clear();
            cleared = true;

            // ⚠️ FALLBACK!
            // also overwrite servlet container headers with empty values
            // to ensure they are effectively removed before re-writing
            for (String name : response.getHeaderNames()) {
                response.setHeader(name, "");
            }
        }

        return cleared;
    }

    /**
     * 📡 Write all buffered headers into the target servlet response.
     *
     * @param response servlet response to receive headers
     */
    void write(HttpServletResponse response);

    /**
     * 📥 Access the buffered headers.
     *
     * @return the local {@link Headers} instance
     */
    Headers getHeaders();

    /**
     * ✅ Determine whether headers have already been written to the response.
     *
     * @return {@code true} if headers were written; {@code false} otherwise
     */
    boolean isWritten();
}
