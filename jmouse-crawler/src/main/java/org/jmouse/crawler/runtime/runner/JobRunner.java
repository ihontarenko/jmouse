package org.jmouse.crawler.runtime.runner;

import org.jmouse.crawler.api.ProcessingEngine;

public interface JobRunner {
    void runUntilDrained(ProcessingEngine engine);
}
