package org.jmouse.meterializer;

import org.jmouse.util.Strings;

import static org.jmouse.core.Verify.nonNull;

/**
 * Immutable representation of a qualified name (QName). 🏷️
 *
 * <p>Encapsulates:</p>
 * <ul>
 *     <li>namespace URI (optional)</li>
 *     <li>prefix (optional)</li>
 *     <li>local name (required)</li>
 * </ul>
 *
 * <h3>Examples</h3>
 *
 * <pre>{@code
 * QName local = QName.local("user");
 * local.qualified();        // "user"
 *
 * QName namespaced = QName.namespace("urn:test", "user");
 * namespaced.hasNamespace(); // true
 *
 * QName full = QName.of("urn:test", "x", "user");
 * full.qualified();          // "x:user"
 * }</pre>
 *
 * <p>
 * All components are normalized via {@link Strings#normalize} (trimmed, null-safe).
 * The {@code name} is mandatory and cannot be {@code null}.
 * </p>
 */
public record QName(String namespace, String prefix, String name) {

    /**
     * Canonical constructor.
     *
     * <p>Normalizes all components and enforces non-null {@code name}.</p>
     *
     * @param namespace namespace URI (nullable)
     * @param prefix    prefix part (nullable)
     * @param name      local name (required)
     */
    public QName {
        nonNull(name, "name");
        name = Strings.normalize(name, String::trim);
        namespace = Strings.normalize(namespace, String::trim);
        prefix = Strings.normalize(prefix, String::trim);
    }

    /**
     * Creates a local (non-namespaced) name.
     *
     * @param name local name
     * @return QName without namespace and prefix
     */
    public static QName local(String name) {
        return of(null, null, name);
    }

    /**
     * Creates a fully configurable QName.
     *
     * @param namespace namespace URI (nullable)
     * @param prefix    prefix (nullable)
     * @param name      local name (required)
     * @return new QName instance
     */
    public static QName of(String namespace, String prefix, String name) {
        return new QName(namespace, prefix, name);
    }

    /**
     * Creates a QName with namespace but without prefix.
     *
     * @param namespace namespace URI
     * @param name      local name
     * @return QName without prefix
     */
    public static QName namespace(String namespace, String name) {
        return of(namespace, null, name);
    }

    /**
     * Returns a qualified representation.
     *
     * <p>
     * If prefix is present → {@code prefix:name},
     * otherwise → {@code name}.
     * </p>
     *
     * @return qualified name string
     */
    public String qualified() {
        return prefix == null ? name : (prefix + ":" + name);
    }

    /**
     * Indicates whether namespace is defined.
     *
     * @return {@code true} if namespace is non-empty
     */
    public boolean hasNamespace() {
        return Strings.isNotEmpty(namespace);
    }

}