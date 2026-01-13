package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;

public record DeadLetterEntry(CrawlTask task, DeadLetterItem item) {}
