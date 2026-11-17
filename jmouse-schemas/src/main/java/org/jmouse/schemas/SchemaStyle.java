package org.jmouse.schemas;

import java.util.function.Function;

/**
 * How a schema is written: where its objects stand, how an empty value stands, how an enum's constants are spelt in the JSON it
 * describes, and what else its owner marks on it — a settings window's titles and bounds, say — through the generator's own
 * configuration.
 *
 * @param nesting   where the objects inside it are written
 * @param absence   how a value with nothing in it stands: null, or left out
 * @param enumWords the word a constant is written as in the JSON the schema describes
 * @param marks     what the owner adds to the generator's configuration; nothing for a plain model
 */
public record SchemaStyle(Nesting nesting, Absence absence, Function<Enum<?>, String> enumWords, SchemaMarks marks) {

    /** A model as Jackson writes it, its objects each defined once: every property there, null where empty; an enum by its constants' names. */
    public static SchemaStyle defined() {
        return new SchemaStyle(Nesting.DEFINED, Absence.WRITTEN_AS_NULL, Enum::name, SchemaMarks.NONE);
    }

    /** The same style, its enums spelt by these words. */
    public SchemaStyle spelling(Function<Enum<?>, String> words) {
        return new SchemaStyle(nesting, absence, words, marks);
    }

    /** The same style, its objects written where they are used. */
    public SchemaStyle inline() {
        return new SchemaStyle(Nesting.INLINE, absence, enumWords, marks);
    }

    /** The same style, its empty values standing as this says. */
    public SchemaStyle absent(Absence how) {
        return new SchemaStyle(nesting, how, enumWords, marks);
    }

    /** The same style, with what else its owner marks. */
    public SchemaStyle marking(SchemaMarks more) {
        return new SchemaStyle(nesting, absence, enumWords, more);
    }
}
