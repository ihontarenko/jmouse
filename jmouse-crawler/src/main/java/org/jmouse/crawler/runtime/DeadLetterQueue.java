package org.jmouse.crawler.runtime;

import java.util.List;

public interface DeadLetterQueue {

    void put(ProcessingTask task, DeadLetterItem item);

    List<DeadLetterEntry> pollBatch(int max);

    int size();

}
