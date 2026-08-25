package org.jmouse.query.sql;

import org.jmouse.query.translate.UnsupportedQueryException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which mappings live in the same place.
 *
 * <h2>⚠️ The question a join asks is not "can this backend join"</h2>
 *
 * <p>It is <strong>are these two mappings in the same place</strong>. A structure mapped to MySQL and one
 * mapped to a CSV file cannot be joined by a database, because the database has never heard of the file —
 * and nothing about the language, the translator or the dialect changes that.</p>
 *
 * <p>So a mapping is registered under a <em>target</em>: a database, a file, a list somebody handed in.
 * Two mappings sharing one are joinable; two mappings in different ones are refused, naming both.</p>
 *
 * <h2>⚠️ Why this is not a second registry inside the translator</h2>
 *
 * <p>A {@link SqlTranslator} is deliberately over ONE source — that is what makes it a translator for a
 * destination rather than a place where sources are looked up. This holds the map; the translator is
 * handed the answer.</p>
 *
 * <h2>⚠️ An unregistered mapping is not homeless</h2>
 *
 * <p>Everything registered without a target named for it belongs to {@link #DEFAULT}. That keeps every
 * document that never heard of targets working exactly as it did — one place, everything joinable — and
 * makes declaring a second target the moment somebody actually has one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Targets {

    /** Where a mapping lives when nobody said otherwise. */
    public static final String DEFAULT = "default";

    private final Map<String, String> places;

    private Targets(Map<String, String> places) {
        this.places = Map.copyOf(places);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Everything in one place — what a product with one database has. */
    public static Targets single(Set<String> mappings) {
        Builder builder = builder();

        mappings.forEach(mapping -> builder.mapping(DEFAULT, mapping));

        return builder.build();
    }

    /** Where this mapping lives. */
    public String of(String mapping) {
        return places.getOrDefault(mapping, DEFAULT);
    }

    /** Every mapping registered under one target. */
    public Set<String> in(String target) {
        Set<String> mappings = new LinkedHashSet<>();

        places.forEach((mapping, place) -> {
            if (place.equals(target)) {
                mappings.add(mapping);
            }
        });

        return mappings;
    }

    /** Whether these two can appear in one statement. */
    public boolean together(String one, String other) {
        return of(one).equals(of(other));
    }

    /**
     * Refuses two mappings that are not in the same place, naming both and where each is.
     *
     * <p>⚠️ There is a deliberate escape hatch — reading each side and joining them in memory — and it is
     * <strong>asked for explicitly and never chosen automatically</strong>. "The query worked, it just
     * read two million rows" is not a decision an engine gets to make on somebody's behalf.</p>
     *
     * @param one   the outer mapping
     * @param other the one being joined to it
     */
    public void requireTogether(String one, String other) {
        if (together(one, other)) {
            return;
        }

        throw new UnsupportedQueryException(
                ("'%s' is in '%s' and '%s' is in '%s', so one statement cannot reach both; "
                 + "read each side and join them in memory if that is what you want").formatted(
                        one, of(one), other, of(other)));
    }

    /** What a caller can see about the arrangement. */
    public Optional<String> target(String mapping) {
        return Optional.ofNullable(places.get(mapping));
    }

    public static final class Builder {

        private final Map<String, String> places = new LinkedHashMap<>();

        /** Puts one mapping in one place. */
        public Builder mapping(String target, String mapping) {
            places.put(mapping, target);

            return this;
        }

        /** Puts several mappings in one place. */
        public Builder target(String target, String... mappings) {
            for (String mapping : mappings) {
                places.put(mapping, target);
            }

            return this;
        }

        public Targets build() {
            return new Targets(places);
        }
    }
}
