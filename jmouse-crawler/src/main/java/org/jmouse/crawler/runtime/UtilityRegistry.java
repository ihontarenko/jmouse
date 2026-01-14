package org.jmouse.crawler.runtime;

public interface UtilityRegistry {
    <T> T get(Class<T> type);
}
