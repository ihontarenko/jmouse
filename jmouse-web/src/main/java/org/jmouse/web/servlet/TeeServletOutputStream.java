package org.jmouse.web.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.*;

public class TeeServletOutputStream extends ServletOutputStream {

    private final ServletOutputStream delegate;
    private final OutputStream        sink;

    public TeeServletOutputStream(ServletOutputStream delegate, ByteArrayOutputStream sink) {
        this.delegate = delegate;
        this.sink = sink;
    }

    @Override
    public boolean isReady() {
        return delegate.isReady();
    }

    @Override
    public void setWriteListener(WriteListener listener) {
        delegate.setWriteListener(listener);
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
        sink.write(b);
    }

}
