package org.jmouse.core.matcher;

import java.util.*;

final class MatchResult implements Match {

    private static final MatchResult           MISS = new MatchResult(false, Map.of());
    private final        boolean               matched;
    private final        Map<Class<?>, Object> bag;

    private MatchResult(boolean matched, Map<Class<?>, Object> bag) {
        this.matched = matched;
        this.bag = bag;
    }

    public static MatchResult miss() {
        return MISS;
    }

    public static MatchResult hit() {
        return new MatchResult(true, Map.of());
    }

    public static MatchResult copyOf(Match other) {
        if (other instanceof MatchResult match) {
            return match;
        }

        if (other.missed()) {
            return miss();
        }

        Map<Class<?>, Object> bag = new LinkedHashMap<>();
        for (Class<?> type : other.types()) {
            other.get(type.asSubclass(Object.class))
                    .ifPresent(value -> bag.put(type, value));
        }

        return new MatchResult(true, Map.copyOf(bag));
    }

    @Override
    public boolean matched() {
        return matched;
    }

    @Override
    public <T> Optional<T> get(Class<T> type) {
        if (missed()) {
            return Optional.empty();
        }

        Object exact = bag.get(type);

        if (exact != null) {
            return Optional.of(type.cast(exact));
        }

        for (Object value : bag.values()) {
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
        }

        return Optional.empty();
    }

    @Override
    public Set<Class<?>> types() {
        return bag.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public MatchResult attach(Object value) {
        if (missed()) {
            return this;
        }

        if (value == null) {
            return this;
        }

        Class<Object> type = (Class<Object>) value.getClass();

        return attach(type, value);
    }

    @Override
    public <T> MatchResult attach(Class<T> type, T value) {
        if (missed()) {
            return this;
        }

        if (value == null) {
            return this;
        }

        Map<Class<?>, Object> next = new LinkedHashMap<>(bag);
        next.put(type, value);

        return new MatchResult(true, Collections.unmodifiableMap(next));
    }

    /**
     * Merge facets from another match (keeps this.matched).
     */
    MatchResult mergeFrom(Match other) {
        if (this.missed()) {
            return this;
        }

        if (other.missed()) {
            return this;
        }

        Map<Class<?>, Object> next = new LinkedHashMap<>(bag);

        for (Class<?> type : other.types()) {
            other.get(type.asSubclass(Object.class)).ifPresent(v -> next.put(type, v));
        }

        return new MatchResult(true, Collections.unmodifiableMap(next));
    }

    @Override
    public String toString() {
        if (missed()) {
            return "Match[✗]";
        }

        return "Match[✓ " + bag + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof MatchResult match)) {
            return false;
        }

        return matched == match.matched && Objects.equals(bag, match.bag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matched, bag);
    }
}
