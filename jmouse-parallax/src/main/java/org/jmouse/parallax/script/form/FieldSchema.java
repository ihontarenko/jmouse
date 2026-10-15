package org.jmouse.parallax.script.form;

import java.util.List;

/**
 * One field of a line: what it is called, what it holds, and what it may hold.
 *
 * @param name     the key the field's value travels under
 * @param label    how a form names it
 * @param type     what it holds, and so its widget
 * @param required whether a line is incomplete without it
 * @param words    for {@link FieldType#WORD} and {@link FieldType#FLAGS}: the words allowed, as written
 * @param kinds    for {@link FieldType#REFERENCE} and {@link FieldType#REFERENCES}: the kinds a referenced document may
 *                 be, as their keywords; empty for any
 */
public record FieldSchema(String name, String label, FieldType type, boolean required, List<String> words,
                          List<String> kinds) {

    public FieldSchema {
        words = List.copyOf(words);
        kinds = List.copyOf(kinds);
    }

    static FieldSchema of(String name, String label, FieldType type) {
        return new FieldSchema(name, label, type, false, List.of(), List.of());
    }

    static FieldSchema required(String name, String label, FieldType type) {
        return new FieldSchema(name, label, type, true, List.of(), List.of());
    }

    static FieldSchema word(String name, String label, boolean required, List<String> words) {
        return new FieldSchema(name, label, FieldType.WORD, required, words, List.of());
    }

    static FieldSchema flags(String name, String label, List<String> words) {
        return new FieldSchema(name, label, FieldType.FLAGS, false, words, List.of());
    }

    static FieldSchema reference(String name, String label, FieldType type, boolean required, List<String> kinds) {
        return new FieldSchema(name, label, type, required, List.of(), kinds);
    }
}
