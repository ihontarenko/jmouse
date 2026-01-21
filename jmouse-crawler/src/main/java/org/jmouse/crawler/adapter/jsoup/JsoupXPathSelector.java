package org.jmouse.crawler.adapter.jsoup;

import org.jmouse.crawler.api.ParsedDocument;
import org.jmouse.crawler.selector.XPathSelector;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class JsoupXPathSelector implements XPathSelector {

    private final XPath xpath = XPathFactory.newInstance().newXPath();

    @Override
    public List<String> strings(ParsedDocument document, String expr) {
        try {
            Document     jsoupDocument = ((JsoupParsedDocument) document).asW3C();
            NodeList     nodes         = (NodeList) xpath.evaluate(expr, jsoupDocument, XPathConstants.NODESET);
            List<String> result        = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); i++) {
                result.add(nodes.item(i).getTextContent());
            }

            return result;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<URI> links(ParsedDocument document, String expr) {
        try {
            Document  jsoupDocument = ((JsoupParsedDocument) document).asW3C();
            NodeList  nodes         = (NodeList) xpath.evaluate(expr, jsoupDocument, XPathConstants.NODESET);
            List<URI> result        = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); i++) {
                result.add(URI.create(nodes.item(i).getTextContent()));
            }

            return result;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }

    }
}

