package org.jmouse.query.translate;

/**
 * One thing a query may ask for that a backend may or may not be able to do.
 *
 * <h2>⚠️ Named, not enumerated — and that is the whole point</h2>
 *
 * <p>This was an {@code enum}, and an enum is closed: a product could not add to it. So a backend over
 * a search engine wanting {@code score}, or a row backend wanting to say it can honour a subquery but
 * not a correlated one, had nowhere to say so — and the only way to add a capability was to edit this
 * library, which is precisely what a pluggable engine is supposed to make unnecessary.</p>
 *
 * <p>A capability is now a name. The nine below are the language's own; anybody may mint another.</p>
 *
 * <h2>⚠️ A dot means it belongs to somebody else</h2>
 *
 * <p>{@code filter} is the language's, forever. {@code elastic.score} belongs to whoever registered it.
 * Without that rule, the first product to declare {@code score} collides with the language declaring
 * {@code score} a year later — silently, because both are just strings. So an unqualified name is
 * reserved, and a product's own capability carries the namespace it came from.</p>
 *
 * @param name the capability's name, lower-case, optionally qualified with a namespace and a dot
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record Capability(String name) {

    /** {@code where} — filtering rows. A backend that cannot do this is not a backend. */
    public static final Capability FILTER = new Capability("filter");

    /** {@code order} — sorting. */
    public static final Capability SORT = new Capability("sort");

    /** {@code fetch} — returning something other than whole rows. */
    public static final Capability PROJECT = new Capability("project");

    /** {@code group} and {@code having} — gathering rows and filtering the groups. */
    public static final Capability AGGREGATE = new Capability("aggregate");

    /** Reaching a value that lives in another table, or joining two structures. */
    public static final Capability JOIN = new Capability("join");

    /** A named view standing in for a set — {@code in someView}. */
    public static final Capability SUBQUERY = new Capability("subquery");

    /**
     * An existence test that refers outward, evaluated once per row.
     *
     * <p>⚠️ Deliberately separate from {@link #SUBQUERY}: a backend reading rows can honour the
     * uncorrelated form by running the inner query first and binding its result, and cannot honour this
     * one at all — over a file of any size a scan inside a scan is not slow, it is a hang.</p>
     */
    public static final Capability CORRELATE = new Capability("correlate");

    /** Reading text as a number, and taking a value apart. */
    public static final Capability CONVERT = new Capability("convert");

    /** {@code now()} and durations. */
    public static final Capability CLOCK = new Capability("clock");

    /** {@code limit} — bringing back at most so many rows. */
    public static final Capability LIMIT = new Capability("limit");

    public Capability {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a capability needs a name");
        }

        if (!name.equals(name.trim()) || name.startsWith(".") || name.endsWith(".")) {
            throw new IllegalArgumentException(
                    "'%s' is not a capability name; write it as 'filter' or 'elastic.score'"
                            .formatted(name));
        }
    }

    /**
     * A capability by name — for a backend contributing one the language does not know.
     *
     * @param name lower-case, and qualified with a dot when it is not the language's own
     * @return the capability
     */
    public static Capability of(String name) {
        return new Capability(name);
    }

    /**
     * A capability belonging to a namespace — {@code named("elastic", "score")} is
     * {@code elastic.score}.
     *
     * @param namespace who it belongs to
     * @param name      what it is called there
     * @return the qualified capability
     */
    public static Capability named(String namespace, String name) {
        return new Capability("%s.%s".formatted(namespace, name));
    }

    /** Whether this capability belongs to somebody other than the language. */
    public boolean isQualified() {
        return name.indexOf('.') >= 0;
    }

    /** Who it belongs to, or an empty string when it is the language's own. */
    public String namespace() {
        int dot = name.indexOf('.');

        return dot < 0 ? "" : name.substring(0, dot);
    }

    @Override
    public String toString() {
        return name;
    }
}
