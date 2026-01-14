package org.jmouse.crawler.spi;

public interface Fetcher {
    FetchResult fetch(FetchRequest request) throws Exception;
}
