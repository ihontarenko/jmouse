package org.jmouse.crawler.html;

import org.jmouse.crawler.spi.Parser;
import org.jmouse.crawler.spi.ParserRegistry;

import java.util.List;

public final class SimpleParserRegistry implements ParserRegistry {

    private final List<Parser> parsers;

    public SimpleParserRegistry(List<Parser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    @Override
    public Parser resolve(String contentType) {
        for (Parser parser : parsers) {
            if (parser.supports(contentType)) {
                return parser;
            }
        }
        return null;
    }
}
