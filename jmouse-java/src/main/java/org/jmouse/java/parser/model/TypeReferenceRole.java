package org.jmouse.java.parser.model;

/**
 * Where in a declaration header a type name is written. Syntactic only: it says which clause the name
 * stands in, never what the name means.
 */
public enum TypeReferenceRole {

    /** A superclass, or a superinterface of an interface. */
    EXTENDS,

    /** An interface a class, enum or record implements. */
    IMPLEMENTS,

    /** A subtype named in a {@code permits} clause. */
    PERMITS,

    /** The declared type of a field. */
    FIELD_TYPE,

    /** The return type of a method. */
    RETURN_TYPE,

    /** The declared type of a formal parameter. */
    PARAMETER_TYPE,

    /** The declared type of a record component. */
    RECORD_COMPONENT_TYPE,

    /** An exception type in a {@code throws} clause. */
    THROWS,

    /** A bound of a type parameter: {@code T extends Bound}. */
    TYPE_PARAMETER_BOUND,

    /** A type written inside the angle brackets of another, at any depth: {@code Map<Key, List<Value>>}. */
    TYPE_ARGUMENT,

    /** The name of an annotation, on a declaration or on a type use. */
    ANNOTATION
}
