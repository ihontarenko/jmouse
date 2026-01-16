package org.jmouse.crawler.runtime;

public record DeadLetterEntry(ProcessingTask task, DeadLetterItem item) {}
