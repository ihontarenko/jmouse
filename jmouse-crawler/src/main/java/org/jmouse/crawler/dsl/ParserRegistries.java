package org.jmouse.crawler.dsl;

import org.jmouse.crawler.spi.ParserRegistry;

public final class ParserRegistries {
    private ParserRegistries() {}

    public static ParserRegistry noop() {
        return contentType -> null;
    }
}