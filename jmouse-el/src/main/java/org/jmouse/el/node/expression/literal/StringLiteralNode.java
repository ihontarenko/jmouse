package org.jmouse.el.node.expression.literal;

import org.jmouse.core.MimeParser;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.LiteralNode;

/**
 * Represents a literal expression node for string values.
 * <p>
 * This node encapsulates a constant string value and returns it upon evaluation.
 * </p>
 */
public class StringLiteralNode extends LiteralNode<String> {

    /**
     * Constructs a StringLiteralNode with the specified string value.
     *
     * @param value the string literal to encapsulate
     */
    public StringLiteralNode(String value) {
        super(value);
    }

    /**
     * Writes the literal back exactly as it was written.
     *
     * <p>⚠️ <strong>The stored value still carries its quotes</strong> — this node keeps the source
     * spelling and unquotes only in {@link #evaluate}. So the un-parse is the value itself, and adding
     * quotes here would double them: {@code 'kos'} would be written {@code "'kos'"}, which re-parses to
     * a different string and does so silently.</p>
     *
     * <p>A node built in code rather than parsed has no quotes to keep, so one pair is added. That case
     * is rare and the check is cheap; without it a programmatically composed query writes a string as a
     * bare identifier.</p>
     */
    @Override
    public String toSource() {
        String value = getValue();

        if (value == null) {
            return "null";
        }

        return isQuoted(value) ? value : quote(value);
    }

    private static boolean isQuoted(String value) {
        if (value.length() < 2) {
            return false;
        }

        char opening = value.charAt(0);

        return (opening == '\'' || opening == '"') && value.charAt(value.length() - 1) == opening;
    }

    /**
     * ⚠️ A literal holds no escape sequence, so the quote character is chosen rather than escaped. A
     * value carrying both kinds has no spelling at all, and saying so beats emitting text that will not
     * parse.
     */
    private static String quote(String value) {
        boolean holdsSingle = value.indexOf('\'') >= 0;
        boolean holdsDouble = value.indexOf('"') >= 0;

        if (holdsSingle && holdsDouble) {
            throw new UnsupportedOperationException(
                    ("%s cannot be written back to source: a string literal holds no escape sequence, "
                     + "so a value carrying both kinds of quote has no spelling").formatted(value));
        }

        char quote = holdsSingle ? '"' : '\'';

        return quote + value + quote;
    }

    /**
     * Evaluates the literal string within the given evaluation context.
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        String value = (String) super.evaluate(context);

        if (value != null && !value.isBlank() && value.length() >= 2) {
            value = MimeParser.unquote(value);
        }

        return value;
    }
}
