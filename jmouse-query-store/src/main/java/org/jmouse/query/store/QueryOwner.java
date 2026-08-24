package org.jmouse.query.store;

import java.util.Objects;

/**
 * 🏷️ Where a saved query belongs — a board, a workspace, a person, or the installation itself.
 *
 * <h2>⚠️ Polymorphic, rather than an enum of scopes</h2>
 *
 * <p>The obvious shape is a {@code scope} column reading {@code PERSONAL | PROJECT | GLOBAL}. It was
 * refused: every product hangs saved queries off something different — one off a board, another off a
 * workspace, a third off a person's own shelf — and an enum here would mean releasing this library each
 * time one of them found a new thing to hang them off. That is the opposite of what extracting it was
 * for.</p>
 *
 * <p>So the type is <strong>the product's vocabulary, not this library's</strong>: {@code BOARD},
 * {@code WORKSPACE}, {@code MEMBER}, {@code SPACE}. Nothing here reads it; it is a bucket name that
 * comes back out exactly as it went in.</p>
 *
 * <h2>⚠️ {@link #INSTALLATION} is a sentinel, not {@code null}</h2>
 *
 * <p>Uniqueness of a saved query is over its owner and its name. Neither MySQL nor PostgreSQL treats
 * two {@code NULL}s as equal — so with {@code null} meaning "belongs to the installation", the one case
 * where a duplicate name is most likely would be the one case the database never checked.</p>
 *
 * @param type       what kind of thing holds this query, in the product's own words
 * @param identifier which one of them
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record QueryOwner(String type, String identifier) {

    /** The type and identifier standing for "the installation itself, not any one thing in it". */
    public static final String INSTALLATION_KEY = "*";

    /** Longest an owner type or identifier may be, matching the columns. */
    public static final int MAXIMUM_LENGTH = 64;

    public QueryOwner {
        Objects.requireNonNull(type, "a saved query's owner type");
        Objects.requireNonNull(identifier, "a saved query's owner identifier");
    }

    /**
     * 🏛️ Queries the whole installation shares, belonging to nothing narrower.
     *
     * @return the installation-wide owner
     */
    public static QueryOwner installation() {
        return new QueryOwner(INSTALLATION_KEY, INSTALLATION_KEY);
    }

    /**
     * 🏷️ An owner of some kind.
     *
     * @param type       the product's word for the kind of thing
     * @param identifier which one
     * @return the owner
     */
    public static QueryOwner of(String type, Object identifier) {
        return new QueryOwner(type, String.valueOf(identifier));
    }

    /**
     * Whether this is the installation rather than something within it.
     *
     * @return {@code true} for the installation-wide owner
     */
    public boolean isInstallation() {
        return INSTALLATION_KEY.equals(type);
    }

    @Override
    public String toString() {
        return type + ":" + identifier;
    }
}
