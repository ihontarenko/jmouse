package org.jmouse.crawler.api;

import java.net.URI;
import java.util.Map;

public record FetchResult(
        URI finalUrl,
        int status,
        Map<String, String> headers,
        byte[] body,
        String contentType
) {}
