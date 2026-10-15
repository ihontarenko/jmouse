package org.jmouse.parallax.script.form;

import java.util.List;
import java.util.Optional;

/**
 * A jMX text as a form: its head, then each statement of its body with its fields, in the order written.
 *
 * <p>A form is built from the text, never kept beside it: an edit is applied to the text ({@link DocumentForms#apply})
 * and the form built again, so code and form are two windows on one document.</p>
 *
 * @param kind       the document's first word
 * @param title      its title
 * @param statements the statements of its body
 * @param addable    the blocks its kind allows that it does not hold yet, and the declarations
 * @param refusal    where the reader refuses the whole text, when it does
 */
public record DocumentForm(String kind, String title, List<StatementForm> statements, List<String> addable,
                           Optional<Refusal> refusal) {

    public DocumentForm {
        statements = List.copyOf(statements);
        addable = List.copyOf(addable);
    }

    /**
     * One statement: a block with its lines, a body in another language, or a statement of one line.
     *
     * @param index   its position among the statements, which an edit names
     * @param word    its first word
     * @param editor  how it is edited
     * @param line    the line it starts on
     * @param header  the fields between its word and its brace, or of the whole statement when it is one line
     * @param lines   the lines inside its braces
     * @param fields  the fields a new line of it is made of
     * @param body    the text between its braces, for a {@link BlockSchema.Editor#BODY}
     * @param problem why its header cannot be shown as fields, when it cannot; it is then one {@link FieldType#CODE}
     */
    public record StatementForm(int index, String word, BlockSchema.Editor editor, int line, List<FormField> header,
                                List<LineForm> lines, List<FieldSchema> fields, String body, String problem) {

        public StatementForm {
            header = List.copyOf(header);
            lines = List.copyOf(lines);
            fields = List.copyOf(fields);
        }
    }

    /**
     * One line inside a block.
     *
     * @param index   its position in the block, which an edit names
     * @param line    the line of the text it starts on
     * @param fields  its fields; one {@link FieldType#CODE} when it is kept as written
     * @param problem why the reader refuses it, when it does — it is then shown as written
     */
    public record LineForm(int index, int line, List<FormField> fields, String problem) {

        public LineForm {
            fields = List.copyOf(fields);
        }
    }

    /** Where and why the reader refuses a text. */
    public record Refusal(int line, int column, String message) {
    }
}
