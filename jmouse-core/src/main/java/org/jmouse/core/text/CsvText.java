package org.jmouse.core.text;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A table written as comma-separated text: a field quoted when it must be, as RFC 4180 says.
 *
 * <p>⚠️ <strong>A spreadsheet runs a cell that starts like a formula.</strong> Quoting does not stop it. A cell whose text
 * came from outside — a distributor's answer, a document somebody sent — goes through {@link #inert(String)} first.</p>
 */
public final class CsvText {

    /** What makes a field need quotes: a comma, a quote, a line break. */
    private static final Pattern QUOTED = Pattern.compile("[\",\\r\\n]");

    /** What a spreadsheet reads as the start of a formula. */
    private static final Pattern FORMULA = Pattern.compile("^[=+\\-@\\t\\r]");

    private CsvText() {
    }

    /** One line of fields, without its line break. */
    public static String row(List<String> fields) {
        return String.join(",", fields.stream().map(CsvText::field).toList());
    }

    public static String field(String text) {
        return QUOTED.matcher(text).find() ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }

    /** Text from outside as a cell a spreadsheet shows and never runs: an apostrophe before what would start a formula. */
    public static String inert(String text) {
        return FORMULA.matcher(text).find() ? "'" + text : text;
    }
}
