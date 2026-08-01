package org.jmouse.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 🚰 Opens the bytes of a piece of {@link Content}, on demand.
 *
 * <p>Deliberately a supplier rather than a stream: a backend decides when — and whether more than
 * once — it wants to read, and a caller that hands over an already-open stream cannot express
 * "read this again" for a retry.</p>
 */
@FunctionalInterface
public interface ContentStream {

    /**
     * 📥 Open a stream over the content.
     *
     * <p>The caller closes what it opens.</p>
     *
     * @return a fresh stream positioned at the first byte
     * @throws IOException when the source cannot be opened
     */
    InputStream open() throws IOException;
}
