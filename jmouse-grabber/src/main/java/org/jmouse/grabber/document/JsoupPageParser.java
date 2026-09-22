package org.jmouse.grabber.document;

import org.jmouse.core.MediaType;
import org.jmouse.grabber.fetch.PageResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * 📖 HTML, through Jsoup.
 *
 * <p>The response's final address is handed to Jsoup as the base address, which is what makes
 * {@code absUrl} correct on a redirected page and is the whole reason link resolution can live in
 * {@link PageElement#link(String)} rather than in every handler.</p>
 */
final class JsoupPageParser implements PageParser {

    @Override
    public boolean supports(MediaType contentType) {
        if (contentType == null) {
            return true;
        }

        // includes() compares type and subtype and ignores parameters, so a "text/html; charset=..."
        // matches without the charset having to be stripped first.
        if (MediaType.TEXT_HTML.includes(contentType)) {
            return true;
        }

        return MediaType.APPLICATION_XHTML_XML.includes(contentType);
    }

    @Override
    public PageDocument parse(PageResponse response) {
        Document document = Jsoup.parse(response.text(), response.address().toString());

        return new JsoupPageDocument(document, response.address());
    }

}
