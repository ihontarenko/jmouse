package org.jmouse.access.el.condition;

import org.jmouse.access.CardinalityKey;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.CardinalityCounters;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@code count('project')} — how many of something exist here, right now.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' project:create deny when count('project') >= allowance('project')
 * &#64;SPACE:'id-space-01' membership:add deny when count('member', 'SPACE:id-space-01') >= 20
 * </pre>
 *
 * <p>Every seat limit, project limit and member limit is this question, and until now a policy file could
 * not ask it: {@code consumed()} answers <em>how much was spent over time</em> and has no way to say
 * <em>how many there are</em>.
 *
 * <p>With no second argument it counts at the place the rule is attached to. With one — written the way
 * {@link ScopeReference#describe()} writes a place — it counts somewhere else.
 *
 * <h2>⚠️ It is not a spend limit, and the difference lets somebody farm it</h2>
 *
 * <p>A count <strong>goes down</strong> when something is deleted: create ten, delete one, create one.
 * Correct for a seat limit — you freed a seat, you may fill it — and wrong for anything metered. The full
 * table is on {@link CardinalityCounters}, and it is worth reading before writing the first rule.
 *
 * <h2>⚠️ It costs a query, per decision</h2>
 *
 * <p>Unlike {@code consumed()}, which reads one indexed row. See the port's contract.
 */
public class CountFunction implements AccessFunction {

    public static final String NAME = "count";

    private final CardinalityCounters counters;
    private final ScopeCatalog        scopes;
    private final Set<String>         countable;

    public CountFunction(CardinalityCounters counters, ScopeCatalog scopes) {
        this(counters, scopes, Set.of());
    }

    /**
     * @param countable what this product declares it can count. ⚠️ Handed over so a rule naming a kind
     *                  nobody counts is refused <strong>at load</strong> — it would otherwise read zero
     *                  forever, and a limit that silently does not exist is the {@code JMF-9} failure in
     *                  a new place.
     */
    public CountFunction(CardinalityCounters counters, ScopeCatalog scopes, Set<String> countable) {
        this.counters  = counters == null ? CardinalityCounters.empty() : counters;
        this.scopes    = scopes;
        this.countable = countable == null ? Set.of() : Set.copyOf(countable);
    }

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String           kind     = required(arguments);
        ConditionContext decision = ConditionBinding.require(context);
        ScopeReference   place    = placeFor(arguments, decision);

        if (place == null) {
            throw new IllegalStateException(
                    ("this rule is attached to no place, so there is nowhere to count '%s'. Name a place "
                     + "as the second argument — count('%s', 'SPACE:id-space-01') — or attach the rule "
                     + "somewhere.").formatted(kind, kind));
        }

        return counters.count(new CardinalityKey(kind, place));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        // Anything but a literal cannot be checked here, and the honest answer is to decline rather than
        // guess — the same bargain every other verifyArguments makes.
        if (arguments.isEmpty()) {
            return;
        }

        verifyKind(arguments.get(0));

        if (arguments.size() > 1 && arguments.get(1) != null && scopes != null) {
            // Throws with its own sentence naming the scopes that would have worked.
            scopes.parse(arguments.get(1));
        }
    }

    private void verifyKind(String kind) {
        if (kind == null || countable.isEmpty() || countable.contains(kind)) {
            return;
        }

        throw new IllegalArgumentException(
                ("nothing counts '%s', so this would read zero forever and the limit would never be "
                 + "reached. Countable: %s.").formatted(kind, String.join(", ", new TreeSet<>(countable))));
    }

    private ScopeReference placeFor(Arguments arguments, ConditionContext decision) {
        if (arguments.size() < 2 || arguments.get(1) == null) {
            return decision.place();
        }

        if (scopes == null) {
            throw new IllegalStateException(
                    "no scope catalogue was given, so '" + NAME + "' cannot tell which place a rule means");
        }

        return scopes.parse(String.valueOf(arguments.get(1)));
    }

    private static String required(Arguments arguments) {
        if (arguments == null || arguments.isEmpty() || arguments.getFirst() == null) {
            throw new IllegalArgumentException(
                    "count(kind) needs to say what it is counting — for example count('project')");
        }

        return String.valueOf(arguments.getFirst());
    }
}
