package org.jmouse.crawler.api;

public interface Parser {
    boolean supports(String contentType);
    ParsedDocument parse(FetchResult result) throws Exception;
}
