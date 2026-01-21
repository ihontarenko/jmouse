package org.jmouse.crawler.runtime.state.persistence;

import org.jmouse.crawler.api.ProcessingTask;

import java.util.Collection;

public interface FrontierRestoreSupport {
    void clear();
    void offerAll(Collection<ProcessingTask> tasks);
}
