package org.jmouse.parallax.store.subject;

import org.jmouse.parallax.problem.ParallaxProblem;

/**
 * What is being computed and shown: a stored object, a formula typed here and now, or a whole jMX document that is
 * not stored as an object (one a scene or a comparison defines inline). All are computed, represented and drawn the
 * same way — the formula and the document are objects that have not been saved.
 *
 * <p>Its written form, the {@linkplain #reference() reference}, is {@code catalogue:<slug or id>},
 * {@code inline:<jMF source>} or {@code document:<jMX text>}: one string that fits a request body.
 */
public sealed interface ComputationSubject {

    String CATALOGUE_PREFIX = "catalogue:";
    String INLINE_PREFIX = "inline:";
    String DOCUMENT_PREFIX = "document:";

    String reference();

    /** A stored object, named by its slug or its identifier. */
    record Catalogue(String slug) implements ComputationSubject {

        public Catalogue {
            if (slug == null || slug.isBlank()) {
                throw ParallaxProblem.invalid("A catalogue subject names an object.");
            }
        }

        @Override
        public String reference() {
            return CATALOGUE_PREFIX + slug;
        }
    }

    /** A jMF formula that is not stored anywhere. */
    record Inline(String source) implements ComputationSubject {

        public Inline {
            if (source == null || source.isBlank()) {
                throw ParallaxProblem.invalid("An inline subject is a formula; type one.");
            }
        }

        @Override
        public String reference() {
            return INLINE_PREFIX + source;
        }
    }

    /** A jMX document kept as text somewhere other than the catalogue: an inline object of a scene or a comparison. */
    record Document(String source) implements ComputationSubject {

        public Document {
            if (source == null || source.isBlank()) {
                throw ParallaxProblem.invalid("A document subject is a jMX text; write one.");
            }
        }

        @Override
        public String reference() {
            return DOCUMENT_PREFIX + source;
        }
    }

    static ComputationSubject parse(String reference) {
        if (reference == null) {
            throw ParallaxProblem.invalid("Name a subject: catalogue:<slug> or inline:<formula>.");
        }
        if (reference.startsWith(CATALOGUE_PREFIX)) {
            return new Catalogue(reference.substring(CATALOGUE_PREFIX.length()).strip());
        }
        if (reference.startsWith(INLINE_PREFIX)) {
            return new Inline(reference.substring(INLINE_PREFIX.length()));
        }
        if (reference.startsWith(DOCUMENT_PREFIX)) {
            return new Document(reference.substring(DOCUMENT_PREFIX.length()));
        }
        throw ParallaxProblem.invalid("A subject is catalogue:<slug>, inline:<formula> or document:<jMX>, not '" + abbreviated(reference) + "'.");
    }

    private static String abbreviated(String reference) {
        if (reference.length() <= 40) {
            return reference;
        }
        return reference.substring(0, 40) + "…";
    }
}
