package org.jmouse.crawler.spi;

public interface ParserRegistry {
    Parser resolve(String contentType);
}
