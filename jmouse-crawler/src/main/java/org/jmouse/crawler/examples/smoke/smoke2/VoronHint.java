package org.jmouse.crawler.examples.smoke.smoke2;

import org.jmouse.crawler.api.RoutingHint;

public enum VoronHint implements RoutingHint {
    LISTING, PAGINATION, PRODUCT, MEDIA;

    @Override
    public String id() {
        return name();
    }
}
