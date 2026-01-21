package org.jmouse.crawler.api;

import org.jmouse.core.MediaType;

public interface ParsedDocument {

    String type();

    default MediaType mediaType() {
        return MediaType.forString(type());
    }

}
