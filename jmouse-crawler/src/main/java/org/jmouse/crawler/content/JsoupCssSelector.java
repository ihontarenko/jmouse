package org.jmouse.crawler.content;

import org.jmouse.crawler.spi.ParsedDocument;

import java.net.URI;
import java.util.List;

public final class JsoupCssSelector implements CssSelector {

    public static final String HREF_ATTRIBUTE = "href";

    @Override
    public List<String> texts(ParsedDocument document, String css) {
        JsoupParsedDocument jsoupDocument = (JsoupParsedDocument) document;
        return jsoupDocument.document().select(css).eachText();
    }

    @Override
    public List<String> attributes(ParsedDocument document, String css, String attribute) {
        JsoupParsedDocument jsoupDocument = (JsoupParsedDocument) document;
        return jsoupDocument.document().select(css).eachAttr(attribute);
    }

    @Override
    public List<URI> links(ParsedDocument document, String css) {
        JsoupParsedDocument jsoupDocument = (JsoupParsedDocument) document;
        return jsoupDocument.document()
                .select(css)
                .eachAttr(HREF_ATTRIBUTE)
                .stream()
                .map(URI::create)
                .toList();
    }

}
