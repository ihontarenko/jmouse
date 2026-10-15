package org.jmouse.java.parser.model;

/**
 * What a declaration declares; also the kind of a JavaDoc comment's owner.
 */
public enum DeclarationKind {

    /** A package declaration, typically documented in {@code package-info.java}. */
    PACKAGE,

    /** A module declaration in {@code module-info.java}. */
    MODULE,

    /** {@code class} */
    CLASS,

    /** {@code interface} */
    INTERFACE,

    /** {@code enum} */
    ENUM,

    /** {@code record} */
    RECORD,

    /** {@code @interface} */
    ANNOTATION_TYPE,

    /** One variable of a field declaration. */
    FIELD,

    /** A method, including an annotation type element. */
    METHOD,

    /** A constructor with a parameter list. */
    CONSTRUCTOR,

    /** A record's compact canonical constructor, written without a parameter list. */
    COMPACT_CONSTRUCTOR,

    /** A component in a record header. */
    RECORD_COMPONENT,

    /** A constant of an enum. */
    ENUM_CONSTANT,

    /** An instance initializer block. */
    INITIALIZER,

    /** A {@code static} initializer block. */
    STATIC_INITIALIZER;

    /**
     * Returns whether this kind declares a type.
     *
     * @return {@code true} for class, interface, enum, record and annotation type
     */
    public boolean isType() {
        return this == CLASS || this == INTERFACE || this == ENUM || this == RECORD || this == ANNOTATION_TYPE;
    }
}
