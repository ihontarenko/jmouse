package org.jmouse.crawler.runtime.state.persistence;

public interface SnapshotStore<S> {

    S load();

    void save(S snapshot);

}
