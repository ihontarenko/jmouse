package org.jmouse.crawler.api;

public interface ParserRegistry {
    Parser resolve(String contentType);
}
