package org.jmouse.grabber;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 🎒 The baton a page hands to the pages it discovers.
 *
 * <p>A listing page knows which category it is; the product page it links to cannot re-derive that
 * from its own markup. Without somewhere to carry it, every grabber grows a side map keyed by
 * address — so the value travels on the {@link Visit} instead.</p>
 *
 * <p>Immutable. {@link #with(String, Object)} returns a new instance, which is what makes it safe to
 * hand the same attributes to a hundred discovered visits.</p>
 */
public final class Attributes {

    private static final Attributes EMPTY = new Attributes(Collections.emptyMap());

    private final Map<String, Object> values;

    private Attributes(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * 🕳️ The attributes of a visit nobody handed anything to — a seed, usually.
     */
    public static Attributes empty() {
        return EMPTY;
    }

    /**
     * 🎒 One value.
     */
    public static Attributes of(String name, Object value) {
        return empty().with(name, value);
    }

    /**
     * 🎒 Everything in the given map, in its iteration order.
     */
    public static Attributes of(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }

        return new Attributes(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
    }

    /**
     * ➕ This, plus one more value. The receiver is unchanged.
     */
    public Attributes with(String name, Object value) {
        Map<String, Object> merged = new LinkedHashMap<>(values);

        merged.put(name, value);

        return new Attributes(Collections.unmodifiableMap(merged));
    }

    /**
     * ➕ This, plus everything in the given attributes. Values on the argument win.
     */
    public Attributes merge(Attributes other) {
        if (other == null || other.isEmpty()) {
            return this;
        }

        if (isEmpty()) {
            return other;
        }

        Map<String, Object> merged = new LinkedHashMap<>(values);

        merged.putAll(other.values);

        return new Attributes(Collections.unmodifiableMap(merged));
    }

    /**
     * 🔍 The value under this name, whatever its type.
     */
    public Optional<Object> get(String name) {
        return Optional.ofNullable(values.get(name));
    }

    /**
     * 🔍 The value under this name, when it is of the expected type.
     *
     * <p>A value of another type answers empty rather than throwing: an attribute is something a page
     * chose to pass along, and a page reading one it was not given is ordinary.</p>
     */
    public <T> Optional<T> get(String name, Class<T> expectedType) {
        Object value = values.get(name);

        if (expectedType.isInstance(value)) {
            return Optional.of(expectedType.cast(value));
        }

        return Optional.empty();
    }

    /**
     * 🔍 The value under this name as text, or the given default.
     */
    public String text(String name, String defaultValue) {
        Object value = values.get(name);

        if (value == null) {
            return defaultValue;
        }

        return String.valueOf(value);
    }

    public boolean contains(String name) {
        return values.containsKey(name);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, Object> asMap() {
        return values;
    }

    @Override
    public String toString() {
        return "Attributes" + values;
    }

}
