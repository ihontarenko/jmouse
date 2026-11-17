package org.jmouse.schemas;

/** Where a schema says the objects inside it. */
public enum Nesting {

    /** Every object written where it is used: what a form drawn from the schema reads, top to bottom, without following a reference. */
    INLINE,

    /** Every object written once, under {@code $defs}, and referred to wherever it is used: what a reader browses, a type a section. */
    DEFINED
}
