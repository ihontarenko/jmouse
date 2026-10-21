package org.jmouse.parallax.script.form;

import java.util.Map;

/**
 * One change a form makes to a document's text, naming what it changes by its place in the {@link DocumentForm} it
 * was made in.
 *
 * @param action    what the change is
 * @param statement the statement it changes, as {@link DocumentForm.StatementForm#index()}
 * @param line      the line inside that statement, as {@link DocumentForm.LineForm#index()}
 * @param values    the fields' new values, by field name
 * @param text      the new text, for a title, a body, a header kept as written, or the word of a block to add
 */
public record FormEdit(Action action, int statement, int line, Map<String, String> values, String text) {

    public FormEdit {
        values = values == null ? Map.of() : Map.copyOf(values);
        text = text == null ? "" : text;
    }

    public enum Action {
        /** The document's title. */
        SET_TITLE,
        /** The address the document is stored under, written after {@code as}: {@code text} is the new one. */
        SET_ADDRESS,
        /** One line inside a block, from its fields. */
        SET_LINE,
        /** A new line at the end of a block, from its fields. */
        ADD_LINE,
        REMOVE_LINE,
        /** What stands between a block's word and its brace: {@code symbol uses …}. */
        SET_HEADER,
        /** The text between a block's braces: jMF definitions, a jMS program. */
        SET_BODY,
        /** A statement of one line, from its fields. */
        SET_STATEMENT,
        REMOVE_STATEMENT,
        /** A block, empty, or a declaration, at the end of the body: {@code text} is its word. */
        ADD_STATEMENT
    }
}
