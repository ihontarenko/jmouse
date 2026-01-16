package org.jmouse.crawler.spi;

import org.jmouse.core.MediaType;

public interface ParsedDocument {

    String type();

    default MediaType mediaType() {
        return MediaType.forString(type());
    }

}
