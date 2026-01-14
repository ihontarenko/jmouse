package org.jmouse.crawler.runtime;

public record DeadLetterEntry(CrawlTask task, DeadLetterItem item) {}
