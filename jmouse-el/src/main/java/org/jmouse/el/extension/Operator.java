package org.jmouse.el.extension;

import org.jmouse.el.lexer.Token;

/**
 * 🔢 Represents an operator used in expressions.
 * Operators have precedence levels, names, and are associated with a specific token type.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Operator {

    /**
     * ⚖️ Returns the precedence level of this operator.
     * Higher values indicate higher precedence in tag evaluation.
     *
     * @return 🔢 the precedence level of the operator
     */
    int getPrecedence();

    /**
     * 🏷️ Returns the name of the operator.
     *
     * @return 📛 the name of the operator
     */
    String getName();

    /**
     * 🔠 Returns the {@link Token.Type} associated with this operator.
     *
     * @return 🎭 the token type representing this operator
     */
    Token.Type getType();

    /**
     * ✍️ How this operator is written in source — {@code >}, {@code |}, {@code ??}.
     *
     * <p>⚠️ <strong>Not {@link #getName()}, and the difference matters when writing source back
     * out.</strong> A name is a label for a reader — {@code GREATER_THAN}, {@code FILTER} — and a node
     * that printed it produced text the lexer cannot read: {@code ( a GREATER_THAN b )} parses as three
     * identifiers, not a comparison. The first token template is the spelling the lexer accepts, and it
     * is what an un-parse has to emit.</p>
     *
     * <p>Falls back to the name only where a token declares no template at all, which no operator's
     * does; the fallback exists so this can never return {@code null} into a formatted string.</p>
     *
     * @return ⌨️ the operator as it is typed
     */
    default String getSpelling() {
        String[] templates = getType().getTokenTemplates();

        return templates.length > 0 ? templates[0] : getName();
    }

    /**
     * Returns the associated calculator for this operator.
     *
     * @return The calculator instance.
     */
    default Calculator<?> getCalculator() {
        throw new UnsupportedOperationException("No corresponding calculator found for: '%s' operator".formatted(getName()));
    }
}
