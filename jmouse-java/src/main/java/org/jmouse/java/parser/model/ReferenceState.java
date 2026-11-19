package org.jmouse.java.parser.model;

/**
 * Whether a JavaDoc element points at another program element, and whether that was resolved.
 *
 * <p>The structural parser never resolves anything, so every reference it produces is
 * {@link #UNRESOLVED}. The state exists so that a later semantic pass can say otherwise without the
 * model changing shape, and so that nobody mistakes a reference for text that was looked up.
 */
public enum ReferenceState {

    /** The element is text, not a reference: {@code {@code}}, {@code {@literal}}, {@code @since} and the like. */
    NOT_A_REFERENCE,

    /**
     * The element refers to something that has not been looked up: {@code {@link}}, {@code {@linkplain}},
     * {@code {@value}}, {@code @see}, the type in {@code @throws}, and {@code {@inheritDoc}}, whose text lives
     * in a supertype. No text is ever invented for it.
     */
    UNRESOLVED
}
