package org.jmouse.crawler.runtime.state.persistence;

import java.net.URI;
import java.util.Collection;

public interface SeenStoreRestoreSupport {
    void clear();
    void putDiscoveredAll(Collection<URI> discovered);
    void putProcessedAll(Collection<URI> processed);
}
