package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;

/**
 * Default {@link FieldKeyResolver} based on the {@code name} attribute. 🧩
 *
 * <p>
 * Converts HTML form field names into canonical property paths.
 * </p>
 *
 * <p>
 * Example conversions:
 * </p>
 *
 * <pre>{@code
 * user.name           -> user.name
 * user[address]       -> user.address
 * user[address][city] -> user.address.city
 * items[0][price]     -> items.0.price
 * }</pre>
 *
 * <p>
 * The same canonical key is used for both field values and validation errors.
 * </p>
 */
public final class DefaultFieldKeyResolver implements FieldKeyResolver {

    /**
     * Resolves the canonical key used to read/write the field value.
     */
    @Override
    public String resolveValueKey(Node node) {
        return canonicalize(node.getAttribute("name"));
    }

    /**
     * Resolves the canonical key used to access validation errors.
     */
    @Override
    public String resolveErrorKey(Node node) {
        return canonicalize(node.getAttribute("name"));
    }

    /**
     * Normalizes field names into dot-separated property paths.
     */
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