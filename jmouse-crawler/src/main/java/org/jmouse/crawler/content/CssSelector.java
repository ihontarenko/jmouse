package org.jmouse.crawler.content;

import org.jmouse.crawler.spi.ParsedDocument;

import java.net.URI;
import java.util.List;

public interface CssSelector {

    List<String> texts(ParsedDocument document, String css);

    List<String> attributes(ParsedDocument document, String css, String attribute);

    List<URI> links(ParsedDocument document, String css);

}

