package org.jmouse.crawler.route;

import org.jmouse.crawler.api.RoutingHint;

public enum ImageDownloaderHint implements RoutingHint {

    IMAGE_DOWNLOADER;

    @Override
    public String id() {
        return name();
    }

}
