package org.jmouse.crawler.smoke.smoke2;

import org.jmouse.crawler.runtime.RoutingHint;

public enum VoronHint implements RoutingHint {
    LISTING, PAGINATION, PRODUCT;

    @Override
    public String id() {
        return name();
    }
}
