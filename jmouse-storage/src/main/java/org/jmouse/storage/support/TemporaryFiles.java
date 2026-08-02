package org.jmouse.storage.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 🧹 Disposes of the working files a write leaves behind.
 *
 * <p>Every backend that writes through an intermediate file — the local one to avoid publishing a
 * truncated object, an object store because a put must declare its length up front — needs the same
 * cleanup on the same paths, including the failure ones.</p>
 */
public final class TemporaryFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemporaryFiles.class);

    private TemporaryFiles() {
    }

    /**
     * 🗑️ Remove a working file, reporting failure only to the log.
     *
     * <p>Nothing escapes. This runs in a {@code finally} on the way out of a write that may already
     * be failing, and a cleanup that can itself throw turns one failure into two — worse, into the
     * wrong one, since the cleanup's exception would replace the exception that explains what
     * actually went wrong.</p>
     *
     * @param temporary the file to remove, or {@code null} when there is nothing to remove
     */
    public static void discard(Path temporary) {
        if (temporary == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporary);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not discard the working file at {}", temporary, exception);
        }
    }
}
