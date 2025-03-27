package org.jmouse.el.rendering;

import java.io.IOException;
import java.io.Writer;

/**
 * 📄 WriterContent is a Content implementation that streams output directly to a {@link Writer}.
 * <p>
 * Note: This implementation supports appending data but does not support retrieving the content
 * (via {@code getDataArray()} or {@code length()}) or prepending data, as these operations are not applicable
 * to a stream-based writer.
 * </p>
 */
public final class WriterContent implements Content {

    private final Writer writer;
    private       int    length = 0;

    /**
     * Constructs a WriterContent instance with the specified writer.
     *
     * @param writer the writer to which content is written
     */
    public WriterContent(Writer writer) {
        this.writer = writer;
    }

    /**
     * Appends the specified character data to the underlying writer.
     *
     * @param data the character array to append
     */
    @Override
    public void append(char[] data) {
        try {
            length += data.length;
            writer.write(data);
        } catch (IOException e) {
            throw new ContentWriterException("Error appending data to content-writer: " + e.getMessage());
        }
    }

    /**
     * Retrieving the content as a character array is not supported in stream-based WriterContent.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public char[] getDataArray() {
        throw new UnsupportedOperationException("getDataArray is not supported in WriterContent.");
    }

    /**
     * Retrieving the length of the content is not supported in stream-based WriterContent.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public int length() {
        return length;
    }
}
