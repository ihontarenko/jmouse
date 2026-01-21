package org.jmouse.crawler.runtime.politeness;

import org.jmouse.crawler.api.ProcessingTask;

public interface PolitenessKeyResolver<K extends PolitenessKey> {
    K resolve(ProcessingTask task);
}
