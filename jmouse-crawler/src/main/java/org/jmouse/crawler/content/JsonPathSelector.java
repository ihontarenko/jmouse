package org.jmouse.crawler.content;

public interface JsonPathSelector {
    <T> T read(byte[] body, String path);
}
