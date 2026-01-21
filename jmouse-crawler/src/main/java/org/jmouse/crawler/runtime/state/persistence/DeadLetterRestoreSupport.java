package org.jmouse.crawler.runtime.state.persistence;

import org.jmouse.crawler.api.DeadLetterEntry;

import java.util.Collection;

public interface DeadLetterRestoreSupport {
    void clear();
    void putAll(Collection<DeadLetterEntry> entries);
}
