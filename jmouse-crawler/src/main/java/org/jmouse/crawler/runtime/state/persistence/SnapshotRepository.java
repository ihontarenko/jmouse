package org.jmouse.crawler.runtime.state.persistence;

public interface SnapshotRepository<S> {
    S load();
    void save(S snapshot);
}