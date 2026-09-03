package org.jmouse.validator.el.parser;

/**
 * The order the validation parsers are offered a statement in, and why that order is what it is.
 *
 * <p>Dispatch asks every parser {@code supports(cursor)} in priority order and takes the first yes,
 * where a <em>lower</em> number is asked earlier.</p>
 *
 * <p><strong>Keyword-led shapes before the default one.</strong> A check line is what any statement is
 * when it is nothing else, so its parser has to be offered last of this language's — otherwise a
 * {@code when} whose guard opens with an identifier would be read as a field called {@code when}.
 * Each of them still asks a second question of its own ({@code opensBlock}), because a field may
 * legitimately be spelled like a keyword; the order is the safety net, not the mechanism.</p>
 *
 * <p>Every band sits near {@link Integer#MIN_VALUE} because the expression language's own parsers carry
 * no priority and therefore sort last — this language must be asked before {@code OperatorParser}
 * offers to read {@code part_number : required} as arithmetic.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmvParserPriority {

    /**
     * ⚠️ {@link ValidationDocumentParser} is deliberately absent. It is the root the reader hands a
     * whole file to, not one more shape a statement might be, and its {@code supports} stays
     * {@code false} so dispatch never offers it anything.
     */
    public static final int GATE       = Integer.MIN_VALUE + 1000;
    public static final int ALWAYS     = Integer.MIN_VALUE + 1100;
    public static final int WHEN       = Integer.MIN_VALUE + 1200;
    public static final int INVARIANT  = Integer.MIN_VALUE + 1300;

    /** The default statement, therefore last of this language's. */
    public static final int CHECK_LINE = Integer.MIN_VALUE + 2000;

    private JmvParserPriority() {
    }
}
