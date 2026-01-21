package org.jmouse.crawler.api;

import org.jmouse.core.events.EventCategory;

public enum CrawlEventCategory implements EventCategory {

    RUN("run"),
    TASK("task"),
    DLQ("dql"),
    RETRY("retry"),
    DECISION("decision"),
    PIPELINE("pipeline");

    private final String id;

    CrawlEventCategory(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return "";
    }

}
