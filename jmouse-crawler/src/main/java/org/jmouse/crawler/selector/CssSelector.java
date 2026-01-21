package org.jmouse.crawler.selector;

import org.jmouse.crawler.api.ParsedDocument;

import java.net.URI;
import java.util.List;

public interface CssSelector {

    List<String> texts(ParsedDocument document, String css);

    List<String> attributes(ParsedDocument document, String css, String attribute);

    List<URI> links(ParsedDocument document, String css);

}

