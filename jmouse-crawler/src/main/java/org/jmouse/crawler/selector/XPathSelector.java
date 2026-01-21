package org.jmouse.crawler.selector;

import org.jmouse.crawler.api.ParsedDocument;

import java.net.URI;
import java.util.List;

public interface XPathSelector {

    List<String> strings(ParsedDocument document, String xpath);

    List<URI> links(ParsedDocument document, String xpath);

}