package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

public final class DefaultFieldKeyResolver implements FieldKeyResolver {

    @Override
    public String resolveValueKey(Node node) {
        return canonicalize(node.getAttribute("name"));
    }

    @Override
    public String resolveErrorKey(Node node) {
        return canonicalize(node.getAttribute("name"));
    }

    private String canonicalize(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return fieldName;
        }

        String canonical = fieldName
                .replace("[", ".")
                .replace("]", "");

        while (canonical.contains("..")) {
            canonical = canonical.replace("..", ".");
        }

        if (canonical.startsWith(".")) {
            canonical = canonical.substring(1);
        }

        if (canonical.endsWith(".")) {
            canonical = canonical.substring(0, canonical.length() - 1);
        }

        return canonical;
    }
}