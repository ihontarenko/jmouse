package org.jmouse.script.el.parser;

/**
 * The order the statement parsers are offered a statement in.
 *
 * <p>Dispatch asks every parser {@code supports(cursor)} in priority order and takes the first yes, where a
 * <em>lower</em> number is asked earlier. Keywords first, the bare shape last: every statement opens with a word
 * of its own except an assignment, which opens with a property path and is therefore asked after everything that
 * can identify itself by its first token.</p>
 *
 * <p>Order is a safety net, not the mechanism — the matchers in {@link org.jmouse.script.el.StatementMatcher}
 * are disjoint. The bands sit at {@link Integer#MIN_VALUE} plus an offset because the expression language's own
 * parsers carry no priority and sort last. A dialect's file-scope constructions take bands below 2000 (jMS's
 * {@code script}, {@code behaviour}) and its handlers the 2000 band; the numbers here are unchanged from jMS.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class StatementPriority {

    public static final int INCLUDE = Integer.MIN_VALUE + 1200;

    public static final int FUNCTION = Integer.MIN_VALUE + 2100;

    public static final int BRANCH = Integer.MIN_VALUE + 3000;
    public static final int LOOP   = Integer.MIN_VALUE + 3100;
    public static final int DECLARATION = Integer.MIN_VALUE + 3200;
    public static final int RETURN = Integer.MIN_VALUE + 3300;

    public static final int ASSIGNMENT = Integer.MIN_VALUE + 3900;

    private StatementPriority() {
    }
}
