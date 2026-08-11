package org.jmouse.access.el.parser;

/**
 * The order the policy parsers are offered a statement in, and why that order is what it is.
 *
 * <p>Dispatch asks every parser {@code supports(cursor)} in priority order and takes the first yes,
 * where a <em>lower</em> number is asked earlier. Two rules set the numbers below.</p>
 *
 * <p><strong>Longer shapes first.</strong> A short shape is often the opening of a long one —
 * {@code @SPACE} is how {@code @SPACE:kyiv entry:read} begins, and {@code form:read} is how
 * {@code form:read "Read forms"} begins. Asked in the other order the short parser answers first,
 * consumes half a statement and leaves the rest to fail somewhere unhelpful.</p>
 *
 * <p><strong>Order is a safety net, not the mechanism.</strong> The matchers in
 * {@link org.jmouse.access.el.CursorMatcher} are written so that at most one of them can match any
 * statement; a grammar whose correctness rests on which parser was registered first is a grammar one
 * refactor away from a silent change of meaning. These numbers exist so that if two shapes ever do
 * overlap again, the more specific one is the one that wins.</p>
 *
 * <p>Every band sits below {@link Integer#MIN_VALUE} plus an offset because the expression language's
 * own parsers carry no priority at all and therefore sort last — the policy language must be asked
 * before {@code OperatorParser} offers to read {@code @SPACE} as arithmetic.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ParserPriority {

    /**
     * ⚠️ {@link PolicyDocumentParser} is deliberately absent. It is the root the evaluator hands a
     * whole file to, not one more shape a statement might be, and its {@code supports} stays
     * {@code false} so dispatch never offers it anything.
     *
     * <p>Blocks — each opens with a keyword of its own, so none of them can be confused with another
     * and their relative order carries no meaning beyond reading order.</p>
     */
    public static final int POLICY       = Integer.MIN_VALUE + 1000;
    public static final int SCOPES       = Integer.MIN_VALUE + 1100;
    public static final int PERMISSIONS  = Integer.MIN_VALUE + 1200;
    public static final int ROLE         = Integer.MIN_VALUE + 1300;
    public static final int SUBJECT      = Integer.MIN_VALUE + 1400;
    public static final int CAPABILITIES = Integer.MIN_VALUE + 1500;
    public static final int PLANS        = Integer.MIN_VALUE + 1600;
    public static final int PLAN         = Integer.MIN_VALUE + 1700;
    public static final int ENTITLEMENTS = Integer.MIN_VALUE + 1800;

    /** Statements written inside a block, longest shape first. */
    public static final int GRANT                  = Integer.MIN_VALUE + 2000;
    public static final int ROLE_ASSIGNMENT        = Integer.MIN_VALUE + 2100;
    public static final int SCOPE_DECLARATION      = Integer.MIN_VALUE + 2200;
    public static final int PERMISSION_DECLARATION = Integer.MIN_VALUE + 2300;
    public static final int INCLUDE                = Integer.MIN_VALUE + 2400;
    public static final int CAPABILITY_DECLARATION = Integer.MIN_VALUE + 2500;
    public static final int PAID_CAPABILITIES      = Integer.MIN_VALUE + 2600;
    public static final int ENTITLEMENT            = Integer.MIN_VALUE + 2700;

    /**
     * ⚠️ Asked last of all the statements, because it is the only shape opening with a bare name and
     * therefore the only one that could be offered somebody else's line. Everything with a keyword,
     * an {@code @} or a colon in front of it gets first refusal.
     */
    public static final int PLAN_GRANT = Integer.MIN_VALUE + 2800;

    /**
     * Fragments — a statement parser calls these directly by type. They are registered so a fragment
     * can still be parsed on its own, and they sort last within the language because each one is a
     * prefix of a statement above and must never be offered a whole statement first.
     */
    public static final int SINGLE_SCOPE     = Integer.MIN_VALUE + 3000;
    public static final int PERMISSION_VALUE = Integer.MIN_VALUE + 3100;

    private ParserPriority() {
    }
}
