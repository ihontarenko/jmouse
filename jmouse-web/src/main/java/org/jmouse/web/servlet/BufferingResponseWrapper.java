package org.jmouse.web.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BufferingResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream  buffer;
    private       TeeWriter              teeWriter;
    private       TeeServletOutputStream teeStream;
    private       boolean                writerCalled = false;
    private       boolean                streamCalled = false;

    public BufferingResponseWrapper(HttpServletResponse response) {
        super(response);
        buffer = new ByteArrayOutputStream();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        TeeServletOutputStream stream = teeStream;

        if (writerCalled) {
            throw new IllegalStateException("Writer already called.");
        }

        if (stream == null) {
            stream = new TeeServletOutputStream(super.getOutputStream(), buffer);
            this.teeStream = stream;
            this.streamCalled = true;
        }

        return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        TeeWriter writer = teeWriter;

        if (streamCalled) {
            throw new IllegalStateException("Stream already called.");
        }

        if (writer == null) {
            String encoding = getCharacterEncoding();

            if (encoding == null) {
                encoding = StandardCharsets.ISO_8859_1.name();
            }

            Charset charset = Charset.forName(encoding);

            writer = new TeeWriter(super.getWriter(), buffer, charset, true);
            this.teeWriter = writer;
            this.writerCalled = true;
        }

        return writer;
    }

    public byte[] getByteArray() {
        if (teeWriter != null) {
            teeWriter.flush();
        }
        if (teeStream != null) {
            try {
                teeStream.flush();
            } catch (IOException ignored) {}
        }
        return buffer.toByteArray();
    }

    public void flushInternal() throws IOException {
        if (teeWriter != null) teeWriter.flush();
        if (teeStream != null) teeStream.flush();
    }

}
