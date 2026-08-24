package org.jmouse.access.el.condition;

import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.spi.ScopeHierarchy;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.List;

/**
 * {@code place is inside('space:42')} — whether the rule is standing at that place or somewhere under it.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' document:read allow when place is inside('space:42')
 * &#64;SPACE:'id-space-01' export:bulk deny   when place is not inside('organisation:acme')
 * </pre>
 *
 * <p>⚠️ {@code PlaceView} — everything a condition could see about where it stands until now — is exactly
 * {@code kind} and {@code id}. So a rule attached high in the tree could not narrow itself to one branch,
 * even though {@link ScopeHierarchy} sits in the engine and answers precisely that question.
 *
 * <h2>⚠️ At or below, not below</h2>
 *
 * <p>{@code place is inside('space:42')} holds <strong>when the place is {@code space:42} itself</strong>,
 * as well as when it sits under it.
 *
 * <p>The other reading is defensible and this one is right: somebody writing <em>"inside space 42"</em>
 * means the space and its contents, the way a folder contains itself in every sentence anybody says about
 * folders. Written here in the first paragraph because the reader who assumed the other reading gets a
 * rule that quietly excludes the one place they were thinking of.
 *
 * <h2>⚠️ It is about the RULE's place, not the request's</h2>
 *
 * <p>A condition runs for the place the <em>grant</em> is attached to, which is not necessarily where the
 * request is aimed. That subtlety already existed on {@code place}; this makes it load-bearing rather
 * than incidental.
 *
 * <h2>⚠️ Not the fix for a subtree grant</h2>
 *
 * <p>A grant at a parent place is refused on its children, because the <em>permission axis</em> never asks
 * the {@code ScopeHierarchy}. This test lets a <em>condition</em> ask about the tree. Same interface,
 * different problem, and mistaking one for the other means the real one stays open.
 */
public class InsideTest implements AccessTest {

    public static final String NAME = "inside";

    private final ScopeHierarchy hierarchy;
    private final ScopeCatalog   scopes;

    public InsideTest(ScopeHierarchy hierarchy, ScopeCatalog scopes) {
        this.hierarchy = hierarchy == null ? ScopeHierarchy.flat() : hierarchy;
        this.scopes    = scopes;
    }

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        ScopeReference wanted   = requireScopes().parse(required(arguments));
        ConditionContext decision = ConditionBinding.require(context);
        ScopeReference   standing = decision.place();

        if (standing == null) {
            throw new IllegalStateException(
                    ("this rule is attached to no place, so there is nothing to ask whether it is inside "
                     + "'%s'.").formatted(wanted.describe()));
        }

        return standing.equals(wanted) || hierarchy.containing(standing).contains(wanted);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        // A place written as anything but a literal cannot be read here, and the honest answer is to
        // decline to check rather than to guess — the same bargain every other verifyArguments makes.
        if (arguments.isEmpty() || arguments.get(0) == null) {
            return;
        }

        // Throws with its own sentence naming the scopes that would have worked.
        requireScopes().parse(arguments.get(0));
    }

    /**
     * ⚠️ Without a vocabulary this cannot answer, and must not pretend to.
     *
     * <p>A catalogue is not optional the way a hierarchy is: a flat hierarchy is a real answer — nothing
     * contains anything — while an absent catalogue means the place a rule named cannot be read at all.
     */
    private ScopeCatalog requireScopes() {
        if (scopes == null) {
            throw new IllegalStateException(
                    "no scope catalogue was given, so '" + NAME + "' cannot tell which place a rule means");
        }

        return scopes;
    }

    private static String required(Arguments arguments) {
        if (arguments == null || arguments.isEmpty() || arguments.getFirst() == null) {
            throw new IllegalArgumentException(
                    "inside needs a place — for example `place is inside('space:42')`");
        }

        return String.valueOf(arguments.getFirst());
    }
}
