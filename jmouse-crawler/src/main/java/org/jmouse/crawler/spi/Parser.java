package org.jmouse.crawler.spi;

public interface Parser {
    boolean supports(String contentType);
    ParsedDocument parse(FetchResult result) throws Exception;
}
