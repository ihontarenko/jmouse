package org.jmouse.crawler.html;

public interface JsonPathSelector {
    <T> T read(byte[] body, String path);
}
