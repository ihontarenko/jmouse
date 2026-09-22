package org.jmouse.grabber.fetch;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

/**
 * 🌐 How a page is obtained.
 *
 * <p>One method, deliberately. This is the seam a browser engine slots into when a site needs one:
 * everything above it — routes, handlers, extraction, the journal — is written against
 * {@link PageResponse} and does not change.</p>
 *
 * <p>A transport failure throws; an unwelcome status does not. See {@link PageResponse}.</p>
 */
@FunctionalInterface
public interface PageFetcher {

    /**
     * 🌐 The standard fetcher, over the JDK's own HTTP client.
     */
    static PageFetcher http() {
        return new HttpPageFetcher(HttpPageFetcher.Settings.defaults());
    }

    /**
     * 🌐 The standard fetcher, configured.
     */
    static PageFetcher http(HttpPageFetcher.Settings settings) {
        return new HttpPageFetcher(settings);
    }

    /**
     * 📁 Serves pages from a directory instead of the network.
     *
     * <p>For a smoke and for a development loop: iterating on a handler should not mean hitting a
     * live site a hundred times, and a fixture on disk is also the only way to write a repeatable
     * check of a handler's behaviour.</p>
     */
    static PageFetcher fromDirectory(Path directory, UnaryOperator<String> addressToFile) {
        return new DirectoryPageFetcher(directory, addressToFile);
    }

    PageResponse fetch(PageRequest request);

}
