package org.jmouse.crawler.html;

import org.jmouse.crawler.spi.ParsedDocument;
import org.jsoup.nodes.Document;

public record JsoupParsedDocument(Document document) implements ParsedDocument {

    @Override
    public String type() {
        return "text/html";
    }
}
