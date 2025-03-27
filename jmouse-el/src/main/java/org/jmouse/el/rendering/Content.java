package org.jmouse.el.rendering;

import java.io.Writer;

/**
 * 🔣 Represents a container for rendered content.
 * <p>
 * This sealed interface defines methods for appending and prepending character data,
 * retrieving the accumulated content as a character array, and obtaining the current length.
 * Two implementations are provided: {@link ArrayContent} and {@link WriterContent}.
 * </p>
 */
public sealed interface Content permits ArrayContent, WriterContent {

    /**
     * Creates a Content instance backed by an expandable character array.
     *
     * @return a new ArrayContent instance
     */
    static Content array() {
        return new ArrayContent();
    }

    /**
     * Creates a Content instance that writes directly to the specified Writer.
     *
     * @param writer the writer to which content is written
     * @return a new WriterContent instance
     */
    static Content writer(Writer writer) {
        return new WriterContent(writer);
    }

    /**
     * Appends the specified character data to the end of the content.
     *
     * @param data the character array to append
     */
    void append(char[] data);

    /**
     * Returns a copy of the current content as a character array.
     *
     * @return the character array containing the rendered content
     */
    char[] getDataArray();

    /**
     * Returns the number of characters currently stored in the content.
     *
     * @return the length of the content
     */
    int length();

    /**
     * Appends the specified string to the content.
     *
     * @param data the string to append
     */
    default void append(String data) {
        append(data.toCharArray());
    }

    /**
     * Appends the content of another Content instance.
     *
     * @param content the Content to append
     */
    default void append(Content content) {
        append(content.getDataArray());
    }

}
