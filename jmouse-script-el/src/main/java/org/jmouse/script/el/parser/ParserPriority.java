package org.jmouse.script.el.parser;

/**
 * The order jMS's own parsers are offered a statement in, and why that order is what it is. The statements
 * inside a body are the core's, ordered by {@link org.jmouse.script.el.parser.StatementPriority} in the same
 * scale — {@code include} at 1200, between these file-scope bands and the handler.
 *
 * <p>Dispatch asks every parser {@code supports(cursor)} in priority order and takes the first yes,
 * where a <em>lower</em> number is asked earlier. Two rules set the numbers below.</p>
 *
 * <p><strong>Keywords first, the bare shape last.</strong> Every construction in this language opens
 * with a word of its own except an assignment, which opens with a property path — so it is asked after
 * everything that can identify itself by its first token.</p>
 *
 * <p><strong>Order is a safety net, not the mechanism.</strong> The matchers in
 * {@link org.jmouse.script.el.CursorMatcher} and {@link org.jmouse.script.el.StatementMatcher} are written so that at most one of them can match any
 * statement; a grammar whose correctness rests on which parser was registered first is a grammar one
 * refactor away from a silent change of meaning. These numbers exist so that if two shapes ever do
 * overlap, the more specific one is the one that wins.</p>
 *
 * <p>Every band sits at {@link Integer#MIN_VALUE} plus an offset because the expression language's own
 * parsers carry no priority at all and therefore sort last — the script language must be asked before
 * {@code OperatorParser} offers to read {@code for} as a property name.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ParserPriority {

    /**
     * ⚠️ {@link ScriptDocumentParser} is deliberately absent. It is the root the evaluator hands a whole
     * file to, not one more shape a statement might be, and its {@code supports} stays {@code false} so
     * dispatch never offers it anything.
     *
     * <p>File-scope constructions — each opens with a keyword of its own, so their relative order
     * carries no meaning beyond reading order.</p>
     */
    public static final int SCRIPT    = Integer.MIN_VALUE + 1000;
    public static final int BEHAVIOUR = Integer.MIN_VALUE + 1100;

    /** A handler is declared inside a block, just before a function (the core's {@code StatementPriority}). */
    public static final int HANDLER = Integer.MIN_VALUE + 2000;


    private ParserPriority() {
    }
}
