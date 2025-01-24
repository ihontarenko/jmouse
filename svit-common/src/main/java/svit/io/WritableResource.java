package svit.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * Interface for resources that support writing operations.
 */
public interface WritableResource {

    /**
     * Checks if the resource is writable.
     */
    default boolean isWritable() {
        return true;
    }

    /**
     * Returns a {@link Writer} for writing to the resource.
     */
    Writer getWriter();

    /**
     * Returns an {@link OutputStream} for writing to the resource.
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Returns a {@link WritableByteChannel} for writing to the resource.
     */
    default WritableByteChannel writableChannel() throws IOException {
        return Channels.newChannel(getOutputStream());
    }
}
