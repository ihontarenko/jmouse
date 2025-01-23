package svit.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public interface WritableResource {

    default boolean isWritable() {
        return true;
    }

    Writer getWriter();

    OutputStream getOutputStream() throws IOException;

    default WritableByteChannel writableChannel() throws IOException {
        return Channels.newChannel(getOutputStream());
    }

}
