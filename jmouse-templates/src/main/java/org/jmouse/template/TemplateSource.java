package org.jmouse.template;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import static org.jmouse.util.helper.Arrays.expand;

public class TemplateSource implements CharSequence {

    private static final int DEFAULT_CAPACITY = 1024;

    private final String template;
    private       int    lineNumber = 1;
    private       int    offset     = 0;
    private       int    size       = 0;
    private       char[] buffer;

    public TemplateSource(String template, Reader reader) {
        this.template = template;
        this.buffer = new char[DEFAULT_CAPACITY];
        copyReaderIntoCharArray(reader);
    }

    public void append(char[] buffer, int length) {
        System.arraycopy(buffer, 0, this.buffer, size, length);
        size += length;
    }

    public void shift(int length) {
        int index = 0;

        while (index < length) {
            int sizeOfNewline = lengthOfNewline(index);
            index += sizeOfNewline > 0 ? sizeOfNewline : 1;
        }

        this.size -= length;
        this.offset += length;
    }

    public String cutout(int start, int end) {
        return new String(Arrays.copyOfRange(buffer, offset + start, offset + end));
    }

    public String cutout(int length) {
        return cutout(0, length);
    }

    private void copyReaderIntoCharArray(Reader reader) {
        char[] buffer = new char[DEFAULT_CAPACITY * 4];
        int    length;

        try {
            while ((length = reader.read(buffer)) != -1) {
                ensureCapacity(size + length);
                append(buffer, length);
            }
        } catch (IOException exception) {
            throw new TemplateException("Error reading template", exception);
        }
    }

    private void ensureCapacity(int capacity) {
        if (0 > buffer.length - capacity) {
            grow(capacity);
        }
    }

    private void grow(int capacity) {
        buffer = expand(buffer, Math.max(buffer.length << 1, capacity));
    }

    private int lengthOfNewline(int index) {
        char character = charAt(index);
        int  length    = 0;

        if (character == '\r' && charAt(index + 1) == '\n') {
            length = 2;
            this.lineNumber++;
        } else if (character == '\n' || character == '\r') {
            length = 1;
            this.lineNumber++;
        }

        return length;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getTemplateName() {
        return template;
    }

    @Override
    public int length() {
        return size;
    }

    @Override
    public char charAt(int index) {
        return buffer[offset + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return cutout(start, end);
    }

    @Override
    public String toString() {
        return new String(Arrays.copyOfRange(buffer, offset, size + offset));
    }
}
