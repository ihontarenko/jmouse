package org.jmouse.crawler.selector;

public interface JsonPathSelector {
    <T> T read(byte[] body, String path);
}
