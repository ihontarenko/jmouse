package org.jmouse.core.convert;

/**
 * A name a converter may be reached by, when the type pair is not enough to say which one is meant.
 *
 * <h2>⚠️ Why a converter needs a name at all</h2>
 *
 * <p>Looking a converter up by its type pair is the right default and answers nearly every question:
 * <em>how do I turn a {@code BigDecimal} into a {@code Money}</em>. It stops being enough the moment
 * one pair has two answers — a {@code BigDecimal} becoming a {@code Money} as an <strong>amount</strong>
 * and as a <strong>rate</strong> are both legal and are different converters. A pair cannot tell them
 * apart. A name can.</p>
 *
 * <p>The other use is a caller being deliberate: a text mapping writing
 * {@code deliveryAddress | via("shop.address")} is saying <em>which</em> transformation it means, in a
 * file somebody reads.</p>
 *
 * <h2>⚠️ A dot means it belongs to somebody else</h2>
 *
 * <p>This is the same rule {@code org.jmouse.el.translate.Capability} states about a capability — not
 * linked, because {@code jmouse-core} does not depend on {@code jmouse-el} and must not start — and it
 * is deliberately not a <em>different</em> rule: without it, the first product to register
 * {@code address} collides with the framework registering {@code address} a year later — silently,
 * because both are just strings.</p>
 *
 * <p>So an unqualified name is <strong>reserved for the framework's own converters</strong>, and a
 * product's converter carries the namespace it came from. Two rules to remember here instead of one
 * would be a worse cost than the rule itself.</p>
 *
 * @param name the converter's name, optionally qualified with a namespace and a dot
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ConverterName(String name) {

    public ConverterName {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a converter needs a name");
        }

        if (!name.equals(name.trim()) || name.startsWith(".") || name.endsWith(".")) {
            throw new IllegalArgumentException(
                    "'%s' is not a converter name; write it as 'money' or 'shop.address'"
                            .formatted(name));
        }
    }

    /**
     * A converter name, as written.
     *
     * @param name the name
     * @return the converter name
     */
    public static ConverterName of(String name) {
        return new ConverterName(name);
    }

    /**
     * A name belonging to a namespace — {@code named("shop", "address")} is {@code shop.address}.
     *
     * @param namespace who it belongs to
     * @param name      what it is called there
     * @return the qualified name
     */
    public static ConverterName named(String namespace, String name) {
        return new ConverterName("%s.%s".formatted(namespace, name));
    }

    /** Whether this name belongs to somebody other than the framework. */
    public boolean isQualified() {
        return name.indexOf('.') >= 0;
    }

    /** Who it belongs to, or an empty string when it is the framework's own. */
    public String namespace() {
        int dot = name.indexOf('.');

        return dot < 0 ? "" : name.substring(0, dot);
    }

    @Override
    public String toString() {
        return name;
    }
}
