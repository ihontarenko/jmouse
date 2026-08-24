package org.jmouse.access.el.condition;

import org.jmouse.access.Allowance;
import org.jmouse.access.CapabilityCatalog;
import org.jmouse.access.CapabilityResolver;
import org.jmouse.access.CapabilityStanding;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.List;
import java.util.function.Supplier;

/**
 * {@code allowance('ai-token')} — how much of a capability this place was actually sold.
 *
 * <h2>The number was in two places</h2>
 *
 * <pre>
 * &#64;SPACE:'id-space-01' assistant:use deny when consumed('ai-token', '3h') >= 100000
 * </pre>
 *
 * <p>{@code 100000} is in the policy file. The plan that <strong>sells</strong> 100 000 is an
 * entitlement row — {@code @ORGANIZATION:acme allow ai-token 100000 reason "…"} — with an
 * {@link Allowance} on it. So two installations on two plans need two policy files differing by a
 * literal, and the day somebody edits the plan on a screen the rule is wrong and nothing says so.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' assistant:use deny when consumed('ai-token', '3h') >= allowance('ai-token')
 * </pre>
 *
 * <p>One rule, every plan, and the number lives where it is sold.
 *
 * <h2>⚠️ An ungranted capability answers 0, and that is the fail-closed reading</h2>
 *
 * <p>{@link CapabilityStanding.Outcome} has five values and only {@code GRANTED} carries an allowance.
 * Everything else — {@code UNGRANTED}, {@code WITHHELD}, {@code EXPIRED}, {@code NOT_YET} — answers
 * <strong>0</strong>.
 *
 * <p>In the shape this is written for, {@code consumed(…) >= allowance(…)}, zero means the deny holds
 * immediately: <em>you were sold nothing, so you have already used all of it.</em>
 *
 * <p>⚠️ <strong>It is therefore written to be compared from a DENY.</strong> The number is a ceiling,
 * not a grant, and reading it from an allow inverts the meaning of every one of those four outcomes.
 *
 * <h2>⚠️ Unlimited is {@code Long.MAX_VALUE}</h2>
 *
 * <p>So {@code consumed(…) >= allowance(…)} is false forever and an unlimited plan is never throttled.
 *
 * <p>⚠️ There is no arithmetic in a condition, so nobody can write {@code allowance(…) - consumed(…)}
 * and discover that this overflows. Worth knowing anyway: the day arithmetic arrives, this is a
 * landmine.
 *
 * <h2>⚠️ It resolves a second axis inside the condition axis</h2>
 *
 * <p>{@link CapabilityResolver} consults the entitlement store and the scope hierarchy — a
 * <em>different</em> axis from the one running this condition, so there is no recursion. But it is a
 * second resolution per decision, uncached, on a path that already runs last on every request.
 *
 * <p>Measure before assuming it is free. If it is not, the answer is a cache keyed the way the
 * resolver's own is — not a cleverer function.
 */
public class AllowanceFunction implements AccessFunction {

    public static final String NAME = "allowance";

    /** Unlimited, as a number a rule can compare without arithmetic. */
    private static final long UNLIMITED = Long.MAX_VALUE;

    /** Sold nothing, so all of it is already used. See the class javadoc. */
    private static final long NOTHING = 0L;

    private final Supplier<CapabilityResolver> resolver;
    private final CapabilityCatalog            capabilities;

    public AllowanceFunction(CapabilityResolver resolver) {
        this(() -> resolver, CapabilityCatalog.empty());
    }

    public AllowanceFunction(CapabilityResolver resolver, CapabilityCatalog capabilities) {
        this(() -> resolver, capabilities);
    }

    /**
     * ⚠️ <strong>A supplier, not the resolver, and it is not fastidiousness.</strong>
     *
     * <p>The chain is real: the condition compiler collects {@code AccessFunction} beans → this one would
     * need a {@link CapabilityResolver} → which composes every {@code EntitlementStore} → one of which
     * reads entitlements out of the <em>live policy document</em> → which was bound using the condition
     * compiler. Asking for the resolver at construction closes that circle, and a context that will not
     * start is the mildest way it could show up.
     *
     * <p>Nothing here needs a resolver until a rule actually calls {@code allowance(…)}, which is long
     * after everything is built. So take a supplier — {@code ObjectProvider::getObject} on the product
     * side — and the circle never forms.
     */
    public AllowanceFunction(Supplier<CapabilityResolver> resolver, CapabilityCatalog capabilities) {
        this.resolver     = resolver == null ? () -> null : resolver;
        this.capabilities = capabilities == null ? CapabilityCatalog.empty() : capabilities;
    }

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String           capability = required(arguments);
        ConditionContext decision   = ConditionBinding.require(context);
        ScopeReference   place      = decision.place();

        if (place == null) {
            throw new IllegalStateException(
                    ("this rule is attached to no place, so there is nothing to ask what '%s' was sold "
                     + "at.").formatted(capability));
        }

        CapabilityResolver resolved = resolver.get();

        if (resolved == null) {
            throw new IllegalStateException(
                    "no capability resolver was given, so '" + NAME + "' cannot tell what was sold");
        }

        CapabilityStanding standing = resolved.standingOf(capability, place);

        if (standing == null || !standing.isGranted()) {
            return NOTHING;
        }

        return standing.ceiling().map(allowance -> allowance.ceiling().orElse(UNLIMITED)).orElse(UNLIMITED);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        // A capability written as anything but a literal cannot be checked here, and the honest answer
        // is to decline rather than guess — the same bargain every other verifyArguments makes.
        if (arguments.isEmpty() || arguments.get(0) == null || capabilities.isEmpty()) {
            return;
        }

        if (!capabilities.contains(arguments.get(0))) {
            throw new IllegalArgumentException(
                    ("nothing declares a '%s' capability, so this would read as ungranted forever and the "
                     + "rule would refuse everybody. Declared capabilities: %s.")
                            .formatted(arguments.get(0), String.join(", ", capabilities.all())));
        }
    }

    private static String required(Arguments arguments) {
        if (arguments == null || arguments.isEmpty() || arguments.getFirst() == null) {
            throw new IllegalArgumentException(
                    "allowance(capability) needs a capability — for example allowance('ai-token')");
        }

        return String.valueOf(arguments.getFirst());
    }
}
