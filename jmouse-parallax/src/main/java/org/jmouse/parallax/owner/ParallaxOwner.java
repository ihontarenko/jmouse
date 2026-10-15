package org.jmouse.parallax.owner;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * 🏷️ Who something belongs to, named by the host: a type the host understands ({@code WORKSPACE}, {@code PROJECT},
 * {@code ACCOUNT}…) and an identifier in it.
 *
 * <p>Parallax keeps no table of owners; it stores the pair and asks the host about it. The shared catalogue has no
 * owner at all, which is why an owner is optional wherever it is stored. Same shape as the files library's
 * {@code OwnerReference}, and for the same reason: a library that referenced a product's tables could not be
 * adopted by a second product.</p>
 *
 * @param type the host's word for the kind of owner
 * @param id   the owner's identifier in that kind
 */
public record ParallaxOwner(String type, String id) {

    /** The library's own type for a scene, the one place inside an owner that Parallax itself defines. */
    public static final String SCENE = "SCENE";

    public ParallaxOwner {
        requireNonNull(type, "type");
        requireNonNull(id, "id");
    }

    /**
     * 🏗️ An owner.
     *
     * @param type the host's word for the kind of owner
     * @param id   the owner's identifier
     * @return the owner
     */
    public static ParallaxOwner of(String type, String id) {
        return new ParallaxOwner(type, id);
    }

    /**
     * 🔎 An owner written as {@link #toString()} writes it — {@code TYPE:id}.
     *
     * <p>The one form an owner travels in where a single string is all there is room for: an authorization rule names
     * one argument as the row it is about, so a route addressed by type and identifier hands the guard this.</p>
     *
     * @param reference the owner as {@code TYPE:id}; the identifier may itself hold a colon
     * @return the owner, or empty when the text is not one
     */
    public static Optional<ParallaxOwner> parse(String reference) {
        if (reference == null) {
            return Optional.empty();
        }

        int separator = reference.indexOf(':');

        if (separator <= 0 || separator == reference.length() - 1) {
            return Optional.empty();
        }

        return Optional.of(of(reference.substring(0, separator), reference.substring(separator + 1)));
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }
}
