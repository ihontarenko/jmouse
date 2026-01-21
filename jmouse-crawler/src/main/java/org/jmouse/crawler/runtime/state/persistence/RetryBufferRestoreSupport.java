package org.jmouse.crawler.runtime.state.persistence;

import java.util.Collection;

public interface RetryBufferRestoreSupport {
    void clear();
    void scheduleAll(Collection<RetryRecord> records);
}
