package org.jmouse.script;

import org.jmouse.script.event.AmendableEvent;

/**
 * Which event a rule is amending right now, and which rule is running.
 *
 * <h2>⚠️ A THREAD'S STATE, AND DELIBERATELY SO</h2>
 *
 * <p>A facade is a singleton a script calls by name — {@code @reading.title(…)} — and the script names
 * neither the event nor itself. Amendment stages run on the caller's thread, synchronously, one rule
 * after another, so "the event being amended on this thread" and "the rule running on this thread" are
 * exact answers.</p>
 *
 * <p>⚠️ Both are set and cleared in {@code finally} by the only two places that run rules, so nothing
 * leaks into the next rule or the next request. A third place that ran rules and forgot the
 * {@code finally} would hand the next caller somebody else's event — which is why the entry points stay
 * package-private and there are exactly two of them.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class AmendmentScope {

    private static final ThreadLocal<AmendableEvent> EVENT = new ThreadLocal<>();
    private static final ThreadLocal<String>         RULE  = new ThreadLocal<>();
    private static final ThreadLocal<String>         STAGE = new ThreadLocal<>();
    private static final ThreadLocal<String>         SCOPE = new ThreadLocal<>();

    private AmendmentScope() {
    }

    static void enter(AmendableEvent event) {
        EVENT.set(event);
    }

    static void leave() {
        EVENT.remove();
    }

    /**
     * @param rule  the rule about to run, or {@code null} once it has
     * @param stage the moment it runs at
     */
    static void running(String rule, String stage, String scope) {
        if (rule == null) {
            RULE.remove();
            STAGE.remove();
            SCOPE.remove();
        } else {
            RULE.set(rule);
            STAGE.set(stage);

            if (scope == null) {
                SCOPE.remove();
            } else {
                SCOPE.set(scope);
            }
        }
    }

    /**
     * Which tenant the running rule belongs to, or {@code null} for the installation as a whole.
     *
     * <h2>⚠️ THIS IS HOW A FACADE THAT WRITES KNOWS WHERE IT IS, AND IT MUST NOT BE AN ARGUMENT</h2>
     *
     * <p>A facade that took a scope would be a facade through which one tenant's rule acts on another's
     * — and every public method on a facade is callable from any rule anybody writes. So the answer
     * comes from the dispatch that is running, which is the only thing that knows and the only thing a
     * rule cannot reach.
     *
     * <p>⚠️ <strong>Null is a real answer and not a failure.</strong> A single-tenant product runs
     * every rule with no scope, so a facade must treat null as <em>the installation</em> rather than as
     * <em>not known yet</em> — and a facade that refuses on null would refuse on every product but the
     * multi-tenant one.
     *
     * @return the tenant of the running rule, or null
     */
    public static String scope() {
        return SCOPE.get();
    }

    /** The moment the running rule was run at, or {@code null} outside a rule. */
    public static String stage() {
        return STAGE.get();
    }

    /**
     * A line a rule logged, handed to the event it is amending — so the outcome's trace shows it in its
     * place. Nothing happens outside an amendment moment.
     */
    public static void noted(String level, String message) {
        AmendableEvent event = EVENT.get();

        if (event != null) {
            event.note(rule(), level, message);
        }
    }

    /**
     * The event being amended, as the type a facade expects.
     *
     * @param expected the event type the facade is written for
     * @param facade   the facade's name, for the sentence
     * @param <E>      the event type
     * @return the event
     * @throws IllegalStateException when no such moment is running — the rule called a facade at a stage
     *                               where nothing can be changed, and is told so
     */
    public static <E extends AmendableEvent> E current(Class<E> expected, String facade) {
        AmendableEvent event = EVENT.get();

        if (!expected.isInstance(event)) {
            throw new IllegalStateException(
                    "@%s changes something only at the moment it belongs to; nothing it can change is happening here."
                            .formatted(facade));
        }

        return expected.cast(event);
    }

    /** The running rule's name — what every amendment is attributed to. */
    public static String rule() {
        String rule = RULE.get();

        return rule == null ? "a rule" : rule;
    }

}
