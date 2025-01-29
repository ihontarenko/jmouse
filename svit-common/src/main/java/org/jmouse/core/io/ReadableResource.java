package org.jmouse.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Interface for resources that can be read.
 * <p>
 * This interface provides methods to access the resource's contents as a {@link Reader},
 * {@link InputStream}, or {@link ReadableByteChannel}.
 * It includes a default implementation for checking if the resource is readable.
 * </p>
 *
 * @see Reader
 * @see InputStream
 * @see ReadableByteChannel
 */
public interface ReadableResource {

    /**
     * Checks if the resource is readable.
     */
    default boolean isReadable() {
        return true;
    }

    /**
     * Returns a {@link Reader} for reading the contents of the resource.
     */
    Reader getReader();

    /**
     * Returns an {@link InputStream} for reading the contents of the resource.
     *
     * @return an {@link InputStream} for the resource
     * @throws IOException if an I/O error occurs while opening the stream
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns a {@link ReadableByteChannel} for reading the contents of the resource.
     *
     * @return a {@link ReadableByteChannel} for the resource
     * @throws IOException if an I/O error occurs while opening the channel
     */
    default ReadableByteChannel readableChannel() throws IOException {
        return Channels.newChannel(getInputStream());
    }

}
