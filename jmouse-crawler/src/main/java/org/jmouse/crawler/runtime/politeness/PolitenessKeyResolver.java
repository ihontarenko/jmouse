package org.jmouse.crawler.runtime.politeness;

import org.jmouse.crawler.api.ProcessingTask;

public interface PolitenessKeyResolver<K> {
    K resolve(ProcessingTask task);
}
