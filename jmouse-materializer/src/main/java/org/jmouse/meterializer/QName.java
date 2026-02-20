package org.jmouse.meterializer;

import org.jmouse.util.Strings;

import static org.jmouse.core.Verify.nonNull;

/**
 * Qualified name for XML-aware template rendering.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>{@code name} is required</li>
 *   <li>{@code namespace} may be null/blank (means "no namespace")</li>
 *   <li>{@code prefix} may be null/blank (no prefix)</li>
 * </ul>
 */
public record QName(String namespace, String prefix, String name) {

    public QName {
        nonNull(name, "name");
        name = Strings.normalize(name, String::trim);
        namespace = Strings.normalize(namespace, String::trim);
        prefix = Strings.normalize(prefix, String::trim);
    }

    public static QName of(String name) {
        return new QName(null, null, name);
    }

    public static QName of(String namespace, String prefix, String name) {
        return new QName(namespace, prefix, name);
    }

    public String qualifiedName() {
        return prefix == null ? name : (prefix + ":" + name);
    }

    public boolean hasNamespace() {
        return Strings.isNotEmpty(namespace);
    }

}