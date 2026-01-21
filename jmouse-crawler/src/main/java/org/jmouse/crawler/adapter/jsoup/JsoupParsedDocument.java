package org.jmouse.crawler.adapter.jsoup;

import org.jmouse.crawler.api.ParsedDocument;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.w3c.dom.Node;

public record JsoupParsedDocument(Document document) implements ParsedDocument {

    @Override
    public String type() {
        return "text/html";
    }

    public org.w3c.dom.Document asW3C() {
        return new W3CDom().fromJsoup(document);
    }

    public Node asW3CNode() {
        return asW3C();
    }

}
