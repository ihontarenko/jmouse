package org.jmouse.crawler.smoke.smoke2;

import org.jmouse.crawler.runtime.CrawlHint;

public enum VoronHint implements CrawlHint {
    LISTING, PAGINATION, PRODUCT;

    @Override public String id() { return name(); }
}
