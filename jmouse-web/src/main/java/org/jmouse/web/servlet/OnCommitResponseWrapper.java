package org.jmouse.web.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 🔔 OnCommitResponseWrapper
 * <p>
 * A response wrapper that triggers a one-time hook {@link #onBeforeCommit()} right
 * before the response is committed or closed.
 *
 * <p>🧠 How it works:</p>
 * <ul>
 *   <li>Wraps {@link ServletOutputStream} and {@link PrintWriter} to track bytes/chars written</li>
 *   <li>Calls {@link #onBeforeCommit()} when either:
 *     <ul>
 *       <li>Content length is reached (if set via {@link #setContentLengthLong(long)} / {@link #setContentLength(int)})</li>
 *       <li>Buffer would overflow (based on {@link #getBufferSize()})</li>
 *       <li>Terminal operations occur: {@code flush()}, {@code close()}, {@link #sendError(int)}, {@link #sendRedirect(String)}, etc.</li>
 *     </ul>
 *   </li>
 *   <li>Ensures the hook is executed <b>once</b> via an internal disable flag</li>
 * </ul>
 *
 * <p>⚠️ Note:</p>
 * <ul>
 *   <li>When writing through the {@link PrintWriter}, tracking uses character counts; depending on encoding,
 *       character count may differ from the actual byte count used by the container.</li>
 *   <li>If precise byte tracking with a writer is critical, override the writer logic accordingly.</li>
 * </ul>
 */
public abstract class OnCommitResponseWrapper extends HttpServletResponseWrapper {

    /**
     * 🚫 Prevents multiple hook executions.
     */
    private boolean disabled;

    /**
     * 📏 Content-Length to honor (if set).
     */
    private long    contentLength;

    /**
     * 🧮 Bytes/characters written (see note above).
     */
    private long    written;

    /**
     * 🏗️ Create a new wrapper for the given response.
     *
     * @param response the underlying {@link HttpServletResponse}
     */
    public OnCommitResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * 🔔 Hook invoked once right before the response is committed.
     * <p>Implementations may add headers, cookies, trailers, logging, etc.</p>
     */
    protected abstract void onBeforeCommit();

    /**
     * 📏 Remember content length (long) and delegate.
     */
    @Override
    public void setContentLengthLong(long length) {
        this.contentLength = length;
        super.setContentLengthLong(length);
    }

    /**
     * 📏 Remember content length (int) and delegate.
     */
    @Override
    public void setContentLength(int length) {
        this.contentLength = length;
        super.setContentLength(length);
    }

    /**
     * 🔌 Wrap output stream to track writes and trigger commit at the right time.
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        var delegate = super.getOutputStream();

        return new ServletOutputStream() {

            @Override
            public void write(int byteValue) throws IOException {
                track(1);
                delegate.write(byteValue);
            }

            @Override
            public void write(byte[] byteValue, int offset, int length) throws IOException {
                track(length);
                delegate.write(byteValue, offset, length);
            }

            @Override
            public void flush() throws IOException {
                doCommit();
                delegate.flush();
            }

            @Override
            public void close() throws IOException {
                doCommit();
                delegate.close();
            }

            @Override
            public boolean isReady() {
                return delegate.isReady();
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                delegate.setWriteListener(writeListener);
            }
        };
    }

    /**
     * ✍️ Wrap writer to track writes and trigger commit at the right time.
     * <p>⚠️ Tracking uses character counts, which may not equal encoded bytes.</p>
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        PrintWriter delegate = super.getWriter();

        return new PrintWriter(delegate) {

            @Override
            public void write(char[] buffer, int offset, int length) {
                track(length);
                super.write(buffer, offset, length);
            }

            @Override
            public void write(int character) {
                track(1);
                super.write(character);
            }

            @Override
            public void write(String value, int offset, int length) {
                track(length);
                super.write(value, offset, length);
            }

            @Override
            public void flush() {
                doCommit();
                super.flush();
            }

            @Override
            public void close() {
                doCommit();
                super.close();
            }
        };
    }

    /**
     * 🚨 Ensure hook runs before error send.
     */
    @Override
    public void sendError(int statusCode) throws IOException {
        doCommit();
        super.sendError(statusCode);
    }

    /**
     * 🚨 Ensure hook runs before error send (with message).
     */
    @Override
    public void sendError(int statusCode, String message) throws IOException {
        doCommit();
        super.sendError(statusCode, message);
    }

    /**
     * 🔀 Ensure hook runs before redirect.
     */
    @Override
    public void sendRedirect(String location) throws IOException {
        doCommit();
        super.sendRedirect(location);
    }

    /**
     * 💨 Ensure hook runs on buffer flush.
     */
    @Override
    public void flushBuffer() throws IOException {
        doCommit();
        super.flushBuffer();
    }

    /**
     * ⛔ Disable the commit hook (after it has run, or for bypass).
     * <p>Useful if downstream code already performed the necessary mutations.</p>
     */
    public void disableCommitHook() {
        this.disabled = true;
    }

    /**
     * 🧮 Track progress and trigger commit when appropriate.
     *
     * @param delta number of bytes (stream) or characters (writer) written
     */
    private void track(long delta) {
        if (!disabled) {
            written += delta;

            boolean isWritten  = contentLength > 0 && written >= contentLength;
            boolean overBuffer = getBufferSize() > 0 && written >= getBufferSize();

            if (isWritten || overBuffer) {
                doCommit();
            }
        }
    }

    /**
     * 🔔 Execute the hook once in a thread-safe manner for this instance.
     */
    private void doCommit() {
        if (!disabled) {
            onBeforeCommit();
            disabled = true;
        }
    }
}
