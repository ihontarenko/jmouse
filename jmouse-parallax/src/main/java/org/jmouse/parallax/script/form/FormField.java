package org.jmouse.parallax.script.form;

/**
 * One field of a line as a form shows it: what it is and the value it holds, as text.
 *
 * @param value the value as the line writes it; names and references separated by commas, flags by spaces
 */
public record FormField(FieldSchema schema, String value) {

    public FormField {
        value = value == null ? "" : value;
    }
}
