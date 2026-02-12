package org.jmouse.crawler.api;

import org.jmouse.core.MediaType;

import java.net.URI;
import java.util.Map;

public record FetchResult(
        URI uri,
        int status,
        Map<String, String> headers,
        byte[] body,
        MediaType mediaType,
        String contentType
) {}
