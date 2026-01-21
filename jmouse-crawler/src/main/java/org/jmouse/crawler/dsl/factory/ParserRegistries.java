package org.jmouse.crawler.dsl.factory;

import org.jmouse.crawler.api.ParserRegistry;

public final class ParserRegistries {
    private ParserRegistries() {}

    public static ParserRegistry noop() {
        return contentType -> null;
    }
}