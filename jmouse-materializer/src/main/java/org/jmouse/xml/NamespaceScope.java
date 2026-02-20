package org.jmouse.xml;

import org.w3c.dom.Element;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

final public class NamespaceScope {

    static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    static final String XMLNS    = "xmlns";

    private final Deque<Map<String, String>> stack = new ArrayDeque<>();

    public NamespaceScope() {
        stack.push(new HashMap<>());
    }

    public void push() {
        stack.push(new HashMap<>(current()));
    }

    public void pop() {
        stack.pop();
    }

    public void declareIfNeeded(Element element, String prefix, String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return;
        }

        String p = normalizePrefix(prefix);

        if (namespace.equals(current().get(p))) {
            return;
        }

        if (p.isEmpty()) {
            element.setAttributeNS(XMLNS_NS, XMLNS, namespace);
        } else {
            element.setAttributeNS(XMLNS_NS, XMLNS + ":" + p, namespace);
        }

        current().put(p, namespace);
    }

    private Map<String, String> current() {
        Map<String, String> top = stack.peek();
        if (top == null) {
            throw new IllegalStateException("NamespaceScope stack is empty");
        }
        return top;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String p = prefix.trim();
        return p.isEmpty() ? "" : p;
    }
}