package org.jmouse.crawler.runtime.state.persistence;

public interface StateBootstrapper {
    void restore();
    void checkpoint();
}
