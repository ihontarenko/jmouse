package org.jmouse.crawler.spi;

import java.net.URI;

public interface SeenStore {
    /**
     * Dedupe at discovery/enqueue time.
     * Returns true if URL was not seen before in "discovered" set and is now reserved.
     */
    boolean markDiscovered(URI url);

    /**
     * Dedupe at execution time.
     * Returns true if URL was not processed before and is now marked processed.
     */
    boolean markProcessed(URI url);

    /**
     * Optional: query helpers.
     */
    boolean isDiscovered(URI url);

    boolean isProcessed(URI url);
}
