package org.jmouse.cad.document;

import java.util.Objects;
import java.util.Optional;

/**
 * A text of the jM language as it enters the engine, wherever it was read from: its origin — what a refusal names —, the
 * kind of document its source declares, and the text. A file declares its kind by its suffix ({@code .symbol.jm}), a
 * store's record by its kind; the language holds the text to the declaration the same way for both, so an extension is
 * a contract, never decoration.
 *
 * @param origin   where the text came from — a path, {@code cad_objects#42} —, as a refusal names it
 * @param declared the kind its source says it holds, by word, or null for a source that declares none
 */
public record DocumentInput(String origin, String declared, String text) {

    /** What a file name ends in after the kind's word: {@code resistor.symbol.jm}. */
    public static final String FILE_SUFFIX = ".jm";

    public DocumentInput {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(text, "text");
    }

    /** A file's text: its kind is the word before {@code .jm} in its name — none for a name that writes no kind. */
    public static DocumentInput file(String name, String text) {
        String declared = null;
        if (name.endsWith(FILE_SUFFIX)) {
            String stem = name.substring(0, name.length() - FILE_SUFFIX.length());
            int dot = stem.lastIndexOf('.');
            if (dot >= 0) {
                declared = stem.substring(dot + 1);
            }
        }
        return new DocumentInput(name, declared, text);
    }

    /** A record's text, of the kind the record says. */
    public static DocumentInput record(String origin, String kind, String text) {
        return new DocumentInput(origin, Objects.requireNonNull(kind, "kind"), text);
    }

    /** A text that declares no kind: what is typed into an editor before it is saved. */
    public static DocumentInput text(String origin, String text) {
        return new DocumentInput(origin, null, text);
    }

    public Optional<String> declaredKind() {
        return Optional.ofNullable(declared);
    }
}
