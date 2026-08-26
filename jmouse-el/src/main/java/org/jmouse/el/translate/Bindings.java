package org.jmouse.el.translate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What a caller supplies by name — {@code currentMember}, a tenant, a date a screen picked.
 *
 * <h2>⚠️ Bound, never spliced</h2>
 *
 * <p>Every value here reaches the compiled form as a <strong>parameter</strong>: one placeholder per
 * value, and one per element for a collection. Nothing is ever concatenated into query text, so there is
 * no injection surface to escape — which is a stronger position than escaping one.</p>
 *
 * <h2>⚠️ Why this is a type and not a {@code Map}</h2>
 *
 * <p>A bare map says nothing about what it is for, and the same method signature then accepts a map of
 * anything. More usefully, the checker needs the <em>names</em> a query may legally mention, and a
 * translator needs the <em>values</em> — one type answers both, so the two can never be given different
 * maps by accident.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Bindings {

    private static final Bindings NONE = new Bindings(Map.of());

    private final Map<String, Object> values;

    private Bindings(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    /** Nothing supplied — a query mentioning a name is refused rather than reading a null. */
    public static Bindings none() {
        return NONE;
    }

    public static Bindings of(Map<String, Object> values) {
        return values.isEmpty() ? NONE : new Bindings(values);
    }

    /**
     * Pairs, in order: a name, its value, a name, its value.
     *
     * <pre>{@code
     * Bindings.of("since", opened, "statuses", List.of("NEW", "OPEN"));
     * }</pre>
     *
     * @param pairs an even number of arguments, every other one a name
     * @return the bindings
     */
    public static Bindings of(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "bindings are names and values in pairs, and this list has an odd number of them");
        }

        Map<String, Object> values = new LinkedHashMap<>();

        for (int index = 0; index < pairs.length; index += 2) {
            if (!(pairs[index] instanceof String name)) {
                throw new IllegalArgumentException(
                        "a binding's name has to be a name; found '%s' where one was expected"
                                .formatted(pairs[index]));
            }

            values.put(name, pairs[index + 1]);
        }

        return of(values);
    }

    /**
     * The same bindings with one more.
     *
     * <p>⚠️ A new object rather than a mutation, so a set of bindings handed to a translator cannot grow
     * a value behind that translator's back after it has been checked.</p>
     */
    public Bindings and(String name, Object value) {
        Map<String, Object> merged = new LinkedHashMap<>(values);

        merged.put(name, value);

        return new Bindings(merged);
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }

    public Object value(String name) {
        return values.get(name);
    }

    /** The names a query is allowed to mention — what the schema check reads. */
    public Set<String> names() {
        return values.keySet();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** The values, for a compiler that still takes a map. */
    public Map<String, Object> asMap() {
        return values;
    }

    @Override
    public String toString() {
        return values.keySet().toString();
    }
}
