package org.jmouse.crawler.content;

import org.jmouse.crawler.spi.ParsedDocument;

import java.net.URI;
import java.util.List;

public interface XPathSelector {

    List<String> strings(ParsedDocument document, String xpath);

    List<URI> links(ParsedDocument document, String xpath);

}