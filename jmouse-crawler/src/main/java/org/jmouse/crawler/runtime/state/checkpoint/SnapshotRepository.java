package org.jmouse.crawler.runtime.state.checkpoint;

public interface SnapshotRepository<S> {

    S load();

    void save(S snapshot);

}