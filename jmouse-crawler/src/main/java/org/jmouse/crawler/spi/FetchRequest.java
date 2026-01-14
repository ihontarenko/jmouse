package org.jmouse.crawler.spi;

import java.net.URI;
import java.util.Map;

public record FetchRequest(URI url, Map<String, String> headers) {}
