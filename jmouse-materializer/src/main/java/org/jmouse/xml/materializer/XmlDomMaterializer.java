package org.jmouse.xml.materializer;

import org.jmouse.meterializer.*;
import org.jmouse.xml.NamespaceConfig;
import org.jmouse.xml.NamespaceScope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.util.Strings.*;

public final class XmlDomMaterializer extends AbstractTemplateMaterializer<Node> {

    private final Document        document;
    private final NamespaceConfig namespaceConfig;
    private final NamespaceScope  namespaces = new NamespaceScope();

    public XmlDomMaterializer(ValueResolver resolver) {
        this(resolver, prefix -> null);
    }

    public XmlDomMaterializer(ValueResolver resolver, NamespaceConfig namespaceConfig) {
        super(resolver);
        this.namespaceConfig = nonNull(namespaceConfig, "namespaceConfig");
        this.document = newDocument();
    }

    private static Document newDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create XML Document", exception);
        }
    }

    private ResolvedQName resolveQName(QName qName) {
        String localName = qName.name();
        String prefix    = nullIfEmpty(normalize(qName.prefix(), String::trim));
        String namespace = nullIfEmpty(normalize(qName.namespace(), String::trim));

        if (namespace == null && prefix != null) {
            namespace = nullIfEmpty(normalize(namespaceConfig.getNamespaceFor(prefix), String::trim));
        }

        String qualifiedName = prefix == null ? localName : ("%s:%s".formatted(prefix, localName));

        return new ResolvedQName(namespace, prefix, localName, qualifiedName);
    }

    public Document document() {
        return document;
    }

    @Override
    protected Node materializeElement(NodeTemplate.Element element, RenderingExecution execution) {
        nonNull(element, "element");
        nonNull(execution, "execution");

        namespaces.push();
        try {
            Element node = createElement(element.qName());

            applyAttributes(node, element.attributes(), execution);

            if (element.children() != null) {
                for (NodeTemplate child : element.children()) {
                    Node childNode = materializeInternal(child, execution);
                    if (childNode != null) {
                        appendChild(node, childNode);
                    }
                }
            }

            return node;
        } finally {
            namespaces.pop();
        }
    }

    private Element createElement(QName qName) {
        nonNull(qName, "qName");

        if (!qName.hasNamespace()) {
            return document.createElement(qName.name());
        }

        Element element = document.createElementNS(qName.namespace(), qName.qualified());

        namespaces.declareIfNeeded(element, qName.prefix(), qName.namespace());

        return element;
    }

    private void applyAttributes(Element element, Map<String, ValueExpression> attributes, RenderingExecution execution) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ValueExpression> entry : attributes.entrySet()) {
            String attribute = entry.getKey();
            Object value     = valueResolver.resolve(entry.getValue(), execution);

            if (value == null) {
                continue;
            }

            element.setAttribute(attribute, String.valueOf(value));
        }
    }

    @Override
    protected Node createTextNode(String text) {
        return document.createTextNode(emptyIfNull(text));
    }

    @Override
    protected Node createContainerNode() {
        return document.createDocumentFragment();
    }

    @Override
    protected Node emptyNode() {
        return document.createDocumentFragment();
    }

    @Override
    protected void appendChild(Node parent, Node child) {
        nonNull(parent, "parent");
        nonNull(child, "child");

        if (child.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
            Node next;
            while ((next = child.getFirstChild()) != null) {
                child.removeChild(next);
                parent.appendChild(next);
            }
            return;
        }

        parent.appendChild(child);
    }

    @Override
    protected Node createElementNode(String tagName) {
        return document.createElement(nonNull(tagName, "tagName"));
    }

    private record ResolvedQName(String namespace, String prefix, String localName, String qualifiedName) {
    }

}