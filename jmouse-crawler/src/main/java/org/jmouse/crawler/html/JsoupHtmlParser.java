package org.jmouse.crawler.html;

import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.ParsedDocument;
import org.jmouse.crawler.spi.Parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.StandardCharsets;

public final class JsoupHtmlParser implements Parser {

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.contains("text/html");
    }

    @Override
    public ParsedDocument parse(FetchResult result) {
        String html = new String(result.body(), StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html, result.finalUrl().toString());
        return new JsoupParsedDocument(document);
    }
}
