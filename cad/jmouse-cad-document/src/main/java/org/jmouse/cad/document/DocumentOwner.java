package org.jmouse.cad.jpa.catalogue;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * 🏷️ Who a document belongs to, named by the host: a type the host understands ({@code WORKSPACE}, {@code PROJECT}…)
 * and an identifier in it.
 *
 * <p>The library keeps no table of owners; it stores the pair and the host is asked about it. The shared catalogue has
 * no owner at all, which is why an owner is optional wherever it is stored. Same shape as the files library's
 * {@code OwnerReference}, and for the same reason: a library that referenced a product's tables could not be adopted
 * by a second product.</p>
 *
 * @param type the host's word for the kind of owner
 * @param id   the owner's identifier in that kind
 */
public record DocumentOwner(String type, String id) {

    public DocumentOwner {
        requireNonNull(type, "type");
        requireNonNull(id, "id");
    }

    public static DocumentOwner of(String type, String id) {
        return new DocumentOwner(type, id);
    }

    /**
     * 🔎 An owner written as {@link #toString()} writes it — {@code TYPE:id}: the one form an owner travels in where a
     * single string is all there is room for, such as the argument an authorization rule names.
     *
     * @param reference the owner as {@code TYPE:id}; the identifier may itself hold a colon
     * @return the owner, or empty when the text is not one
     */
    public static Optional<DocumentOwner> parse(String reference) {
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
