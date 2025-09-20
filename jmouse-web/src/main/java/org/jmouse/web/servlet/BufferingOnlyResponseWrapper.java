package org.jmouse.web.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 📦 Buffer-only {@link HttpServletResponseWrapper}.
 *
 * <p>Captures all response data into an internal buffer during the filter chain.
 * <b>No bytes</b> are written to the underlying {@link HttpServletResponse}
 * until {@link #writeToResponse()} is explicitly called.</p>
 *
 * <h3>Highlights</h3>
 * <ul>
 *   <li>🧲 Collects bytes via {@link #getOutputStream()} or {@link #getWriter()} (mutually exclusive).</li>
 *   <li>🧾 Preserves the requested character encoding when using a writer
 *       (defaults to ISO-8859-1 when none is set).</li>
 *   <li>📤 Emits the buffered body once via {@link #writeToResponse()} and sets {@code Content-Length}.</li>
 * </ul>
 */
public class BufferingOnlyResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(8 * 1024);

    private CapturingServletOutputStream capturingStream;
    private PrintWriter                  capturingWriter;

    private boolean writerCalled = false;
    private boolean streamCalled = false;

    /**
     * 🏗️ Create a new buffering wrapper.
     *
     * @param response the original HTTP response to wrap
     */
    public BufferingOnlyResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * 🔌 Returns a buffering {@link ServletOutputStream}.
     *
     * <p>Mutually exclusive with {@link #getWriter()}.</p>
     *
     * @throws IllegalStateException if {@link #getWriter()} was already called
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writerCalled) {
            throw new IllegalStateException("getWriter() has already been called.");
        }

        if (capturingStream == null) {
            capturingStream = new CapturingServletOutputStream(buffer);
            streamCalled = true;
        }

        return capturingStream;
    }

    /**
     * ✍️ Returns a buffering {@link PrintWriter} honoring the response encoding.
     *
     * <p>Mutually exclusive with {@link #getOutputStream()}.</p>
     *
     * <p>If no character encoding is set, defaults to {@code ISO-8859-1} per Servlet spec.</p>
     *
     * @throws IllegalStateException if {@link #getOutputStream()} was already called
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (streamCalled) {
            throw new IllegalStateException("getOutputStream() has already been called.");
        }

        if (capturingWriter == null) {
            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = StandardCharsets.ISO_8859_1.name();
            }
            Charset charset = Charset.forName(encoding);

            capturingWriter = new PrintWriter(new OutputStreamWriter(buffer, charset), false);
            writerCalled = true;
        }

        return capturingWriter;
    }

    /**
     * 📥 Get the captured response body as raw bytes.
     *
     * <p>Flushes the writer/stream first to ensure all data is present.</p>
     *
     * @return byte array with the buffered content
     */
    public byte[] getByteArray() {
        flushCaptures();
        return buffer.toByteArray();
    }

    /**
     * 📤 Write the buffered content to the underlying response exactly once.
     *
     * <p>Flushes captures, sets {@code Content-Length}, then writes and flushes the servlet output stream.</p>
     *
     * @throws IOException if I/O fails when writing to the response
     */
    public void writeToResponse() throws IOException {
        flushCaptures();

        byte[]              byteArray       = buffer.toByteArray();
        HttpServletResponse servletResponse = (HttpServletResponse) getResponse();

        servletResponse.setContentLength(byteArray.length);

        try (ServletOutputStream servletOutput = servletResponse.getOutputStream()) {
            servletOutput.write(byteArray);
            servletOutput.flush();
        }
    }

    /**
     * ♻️ Reset only the internal buffer (does not affect status/headers).
     */
    @Override
    public void resetBuffer() {
        buffer.reset();
    }

    /**
     * ♻️ Fully reset the wrapper and the underlying response, then clear the buffer.
     */
    @Override
    public void reset() {
        super.reset();
        buffer.reset();
    }

    /**
     * 🔄 No-op flush for the wrapper itself.
     *
     * <p>Use {@link #writeToResponse()} to emit buffered data to the client.</p>
     */
    @Override
    public void flushBuffer() {
        // Intentionally no-op for the wrapper; actual emission happens in writeToResponse().
    }

    /**
     * 🔁 Flush writer/stream into the internal buffer.
     */
    private void flushCaptures() {
        if (capturingWriter != null) {
            capturingWriter.flush();
        }
        if (capturingStream != null) {
            capturingStream.flush();
        }
    }
}
