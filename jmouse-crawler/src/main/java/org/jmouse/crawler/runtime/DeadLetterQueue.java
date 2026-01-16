package org.jmouse.crawler.runtime;

import java.util.List;

public interface DeadLetterQueue {

    void put(CrawlTask task, DeadLetterItem item);

    List<DeadLetterEntry> pollBatch(int max);

    int size();

}
