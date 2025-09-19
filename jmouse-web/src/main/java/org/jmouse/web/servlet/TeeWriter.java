package org.jmouse.web.servlet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

public class TeeWriter extends PrintWriter {

    private final ByteArrayOutputStream sink;
    private final PrintWriter           writer;

    public TeeWriter(PrintWriter writer, ByteArrayOutputStream sink, Charset encoding, boolean autoFlush) {
        super(new OutputStreamWriter(sink, encoding != null ? encoding : Charset.defaultCharset()), autoFlush);
        this.sink = sink;
        this.writer = writer;
    }

    public ByteArrayOutputStream getSink() {
        return sink;
    }

    public byte[] getBytes() {
        return getSink().toByteArray();
    }

    @Override
    public void write(int character) {
        super.write(character);
        writer.write(character);
    }

    @Override
    public void write(char[] buffer) {
        super.write(buffer);
        writer.write(buffer);
    }

    @Override
    public void write(char[] buffer, int offset, int length) {
        super.write(buffer, offset, length);
        writer.write(buffer, offset, length);
    }

    @Override
    public void write(String value) {
        super.write(value);
        writer.write(value);
    }

    @Override
    public void write(String value, int offset, int length) {
        super.write(value, offset, length);
        writer.write(value, offset, length);
    }

    @Override
    public void flush() {
        super.flush();
        writer.flush();
    }

    @Override
    public void close() {
        try {
            super.flush();
        } catch (Exception ignore) {
            System.out.println(ignore);
        }
        try {
            writer.flush();
        } catch (Exception ignore) {
            System.out.println(ignore);
        }
        super.close();
    }

}
