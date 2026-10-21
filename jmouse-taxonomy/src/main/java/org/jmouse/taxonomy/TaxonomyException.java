package org.jmouse.taxonomy;

/**
 * ⚠️ A taxonomy refusing, in words a reader can act on. The kind says how a host answers it: something asked for that
 * is not there, something said that cannot be, or something that would collide with what is.
 */
public sealed class TaxonomyException extends RuntimeException {

    protected TaxonomyException(String message) {
        super(message);
    }

    /** A taxonomy, a category or an assignment that is not there. */
    public static final class NotFound extends TaxonomyException {
        public NotFound(String message) {
            super(message);
        }
    }

    /** A request that cannot be carried out as said: an unknown entity type, a move beneath itself, an empty title. */
    public static final class Invalid extends TaxonomyException {
        public Invalid(String message) {
            super(message);
        }
    }

    /** A request that collides with what is there: a category called the same beside another, a non-empty delete. */
    public static final class Conflict extends TaxonomyException {
        public Conflict(String message) {
            super(message);
        }
    }
}
