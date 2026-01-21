package org.jmouse.crawler.runtime.state.persistence;

import org.jmouse.crawler.api.ProcessingTask;

import java.util.Collection;

public interface InFlightRestoreSupport {
    void clear();
    void putAll(Collection<ProcessingTask> tasks);
}
