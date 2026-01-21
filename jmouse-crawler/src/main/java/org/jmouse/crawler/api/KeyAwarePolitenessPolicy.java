package org.jmouse.crawler.api;

public interface KeyAwarePolitenessPolicy<K> extends PolitenessPolicy {
    K keyOf(ProcessingTask task);
}
