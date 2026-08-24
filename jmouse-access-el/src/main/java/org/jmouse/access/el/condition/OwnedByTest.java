package org.jmouse.access.el.condition;

import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.spi.OwnershipResolver;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * {@code resource is ownedBy(caller)} — the check every product hand-rolls, said once.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' comment:edit allow   when resource is ownedBy(caller)
 * &#64;SPACE:'id-space-01' document:share deny when resource is not ownedBy(caller)
 * </pre>
 *
 * <p>The left operand is the thing; the answer comes from the {@link OwnershipResolver} the product
 * wired. Read that port before writing a rule with this — <em>owns</em> has two honest readings and the
 * port commits to one of them.
 *
 * <h2>⚠️ The argument is for reading, not for choosing</h2>
 *
 * <p>{@code ownedBy(caller)} always asks about the <strong>caller of this decision</strong>, taken from
 * the decision itself. The argument is there because {@code resource is ownedBy} is not a sentence, and
 * it is not consulted.
 *
 * <p>⚠️ Which means {@code resource is ownedBy(somebodyElse)} compiles and answers about the caller
 * anyway — nonsense that loads clean. It is the same wart {@link WorkingHoursTest} carries on its
 * left-hand side, accepted for the same reason and fixed by the same thing: a descriptor that lets a test
 * say what it is about, which is {@code JMF-65}. Do not paper over it here with a special case; two warts
 * with one cause are cheaper to remove than two exceptions.
 *
 * <h2>⚠️ It asks about the resource the AXIS was handed</h2>
 *
 * <p>Not about whatever the rule wrote on the left. The operand reads as {@code resource} because that is
 * what the condition publishes, and the value the port is given is
 * {@link ConditionContext#resource()} — so a rule attached to a route that resolves no resource asks
 * about {@code null}, and a port that cannot answer about {@code null} should throw rather than say
 * {@code false}.
 */
public class OwnedByTest implements AccessTest {

    public static final String NAME = "ownedBy";

    private final OwnershipResolver ownership;

    public OwnedByTest(OwnershipResolver ownership) {
        this.ownership = ownership == null ? OwnershipResolver.nothing() : ownership;
    }

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        ConditionContext decision = ConditionBinding.require(context);

        if (decision.subject() == null) {
            throw new IllegalStateException(
                    "nobody is signed in, so there is no owner to compare against. A rule about owning "
                    + "something belongs on a route that has a caller.");
        }

        return ownership.owns(decision.subject(), decision.resource());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
