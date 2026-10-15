package org.jmouse.parallax.script.form;

/**
 * What kind of value a field of a jMX line holds — and so which widget edits it. A widget belongs to a field type, never
 * to a kind of document: a quantity is edited the same way in a surface, a circuit and a component.
 */
public enum FieldType {

    /** A text in quotes: a summary, a description of a parameter. */
    TEXT,
    /** A {@code """ … """} text that spans lines: Markdown. */
    LONG_TEXT,
    /** A name the document declares: a parameter, a state variable, a port. */
    NAME,
    /** Names separated by commas: tags, the units of a package. */
    NAMES,
    /** One word of a fixed list: an output type, {@code continuous} or {@code discrete}. */
    WORD,
    /** Several words of a fixed list, in any order: {@code log integer animate}. */
    FLAGS,
    /** A jMF expression or a value with its unit, exactly as written: {@code 2 pi}, {@code 4.7 nF}. */
    EXPRESSION,
    /** The slug of another document, of the kinds the field allows. */
    REFERENCE,
    /** Slugs of other documents separated by commas, the first one the default. */
    REFERENCES,
    /** A line or a body kept as written, for a construct no narrower field describes. */
    CODE
}
