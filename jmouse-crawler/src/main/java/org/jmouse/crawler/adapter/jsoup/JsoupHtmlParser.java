package org.jmouse.crawler.adapter.jsoup;

import org.jmouse.crawler.api.FetchResult;
import org.jmouse.crawler.api.ParsedDocument;
import org.jmouse.crawler.api.Parser;
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
        String   html     = new String(result.body(), StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html, result.finalUrl().toString());
        return new JsoupParsedDocument(document);
    }
}
