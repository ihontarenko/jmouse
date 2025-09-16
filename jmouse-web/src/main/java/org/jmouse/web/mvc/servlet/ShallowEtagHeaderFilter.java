package org.jmouse.web.mvc.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.jmouse.web.http.request.IfNoneMatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * 🌊 Shallow ETag filter: wraps the response, buffers body, computes ETag, and
 * applies 304 if {@code If-None-Match} matches.
 *
 * <p>Lightweight alternative to domain-based ETag generation.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Supports multiple alternative filter names (aliases) for registration.</li>
 *   <li>Skips buffering on committed response or error status.</li>
 *   <li>Computes ETag as hex of SHA-256(body).</li>
 * </ul>
 */
public class ShallowEtagHeaderFilter implements Filter {



    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpReq) ||
                !(response instanceof HttpServletResponse httpRes)) {
            chain.doFilter(request, response);
            return;
        }

        // Wrap response to buffer body
        BufferingResponseWrapper wrapper = new BufferingResponseWrapper(httpRes);

        chain.doFilter(request, wrapper);

        if (httpRes.isCommitted() || httpRes.getStatus() >= 300) {
            // skip if already committed or error/redirect
            wrapper.flushToOriginal();
            return;
        }

        byte[] body = wrapper.getBody();
        String etag = "\"" + sha256Hex(body) + "\"";

        IfNoneMatch noneMatch = IfNoneMatch.parse(httpReq.getHeader("If-None-Match")).toNoneMatch();

        String ifNoneMatch = httpReq.getHeader("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.contains(etag)) {
            httpRes.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            httpRes.setHeader("ETag", etag);
            return;
        }

        httpRes.setHeader("ETag", etag);
        wrapper.flushToOriginal();
    }

    @Override
    public void destroy() {
        // no-op
    }

    // --------------------- Helpers ---------------------

    private static String sha256Hex(byte[] body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(body);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Response wrapper that tees output into a byte buffer. */
    private static class BufferingResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream copy = new ByteArrayOutputStream();
        private ServletOutputStream teeStream;
        private PrintWriter teeWriter;

        BufferingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (teeStream == null) {
                teeStream = new ServletOutputStream() {
                    private final ServletOutputStream original = BufferingResponseWrapper.super.getOutputStream();
                    @Override public boolean isReady() { return original.isReady(); }
                    @Override public void setWriteListener(WriteListener listener) { original.setWriteListener(listener); }
                    @Override public void write(int b) throws IOException {
                        copy.write(b);
                        original.write(b);
                    }
                };
            }
            return teeStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (teeWriter == null) {
                teeWriter = new PrintWriter(new OutputStreamWriter(copy, StandardCharsets.UTF_8), true) {
                    private final PrintWriter original = BufferingResponseWrapper.super.getWriter();
                    @Override public void write(char[] buf, int off, int len) {
                        super.write(buf, off, len);
                        original.write(buf, off, len);
                    }
                    @Override public void write(String s, int off, int len) {
                        super.write(s, off, len);
                        original.write(s, off, len);
                    }
                    @Override public void flush() {
                        super.flush();
                        original.flush();
                    }
                };
            }
            return teeWriter;
        }

        byte[] getBody() {
            return copy.toByteArray();
        }

        void flushToOriginal() throws IOException {
            if (teeWriter != null) teeWriter.flush();
            if (teeStream != null) teeStream.flush();
        }
    }
}

