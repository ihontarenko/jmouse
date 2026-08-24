package org.jmouse.query.el.dialect;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.LogicalCalculator;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * {@code and} and {@code or} — the same two operators, spelled the way a person writes them.
 *
 * <h2>⚠️ What this changes is the un-parse, and nothing else</h2>
 *
 * <p>Both spellings have always parsed: {@code T_AND(1070, "&&", "and")} accepts either. What differs is
 * which one comes back out, because {@link Operator#getSpelling()} returns a token's <strong>first</strong>
 * template — so a document somebody wrote as {@code a and b} was written back as {@code a && b}.</p>
 *
 * <p>Harmless in a language for programmers, and wrong here. A query document is read and edited by
 * people who are not writing code; and a builder that saves an edited view writes the whole document
 * back, so the very first save turned every word into a symbol nobody had typed — a diff nobody made.</p>
 *
 * <h2>⚠️ Why not simply reorder the token's templates</h2>
 *
 * <p>Because {@code &&} genuinely <em>is</em> canonical for an expression language, and jMT and
 * {@code .jmp} have no reason to change. The preference belongs to the dialect whose readers want the
 * word, not to the token every dialect shares. Equality is the opposite case and was settled in core:
 * {@code ==} is canonical everywhere, because {@code x = 5} reads as an assignment to anyone with SQL
 * or Java behind them.</p>
 *
 * <h2>⚠️ And not a second renderer either</h2>
 *
 * <p>{@code toSource()} stays the one un-parse there is. This carries the same token, the same
 * calculator and the same precedence as {@link LogicalOperator}'s pair, and answers one question
 * differently — so registering it swaps the spelling for every node the query language builds, without
 * anything downstream knowing a choice was made. A parallel walk that re-printed an expression would be
 * a second renderer to keep in step with the first, which is exactly what the single {@code toSource()}
 * exists to avoid.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum QueryLogicalOperator implements Operator {

    AND(LogicalCalculator.AND, BasicToken.T_AND, "AND", 200, "and"),

    OR(LogicalCalculator.OR, BasicToken.T_OR, "OR", 100, "or");

    private final Calculator<Boolean> calculator;
    private final Token.Type          type;
    private final String              name;
    private final int                 precedence;
    private final String              spelling;

    QueryLogicalOperator(
            Calculator<Boolean> calculator, Token.Type type, String name, int precedence, String spelling) {
        this.calculator = calculator;
        this.type = type;
        this.name = name;
        this.precedence = precedence;
        this.spelling = spelling;
    }

    @Override
    public int getPrecedence() {
        return precedence;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Token.Type getType() {
        return type;
    }

    /** ⚠️ The word, not the symbol — the whole reason this enum exists. */
    @Override
    public String getSpelling() {
        return spelling;
    }

    @Override
    public Calculator<?> getCalculator() {
        return calculator;
    }
}
