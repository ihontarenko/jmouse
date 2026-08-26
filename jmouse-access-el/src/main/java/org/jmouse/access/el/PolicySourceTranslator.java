package org.jmouse.access.el;

import org.jmouse.access.el.node.PolicyNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Capability;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.el.translate.Translator;

/**
 * A policy tree written back out as {@code .jmp} source — the one way out of the tree for this
 * language.
 *
 * <h2>⚠️ This was already a translator; it simply had another name</h2>
 *
 * <p>{@link Translator}'s own javadoc names the failure this closes: an adapter that compiles and a
 * writer that renders are <em>the same operation with a different destination</em>, and two names for
 * one seam is how an un-parse and a compiler drift apart until a document written by one and read by
 * the other means two things. {@code .jmp} was in exactly that position — {@link PolicyWriter} built
 * a tree and rendered it, {@link ExpressionEvaluator#rewrite(String)} parsed a tree and rendered it,
 * and nothing held the two to each other. Both now go through here.</p>
 *
 * <h2>⚠️ What it emits is fixed, byte for byte</h2>
 *
 * <p>Policy revisions are <strong>stored as source text</strong> and an installation can revert to
 * one. So this renders through {@link Node#toSource()} exactly as before and adds nothing of its own:
 * the capability check either throws or does not, and never changes a character. A writer whose
 * output drifts makes an old revision unparseable, which turns revert into a way to break an
 * installation.</p>
 *
 * <h2>⚠️ Bindings are refused rather than ignored</h2>
 *
 * <p>A query is translated with values a caller supplies. A policy names none — a condition reads its
 * variables from the decision it runs inside, not from the writer. Rather than accept a map and
 * quietly drop it, a caller passing one is told, which is the same rule {@link Capabilities} states
 * about a clause: a destination that cannot honour something refuses it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PolicySourceTranslator implements Translator<String> {

    /**
     * ⚠️ Everything a policy file can hold, named one by one rather than asserted in a word.
     *
     * <p>{@link Capabilities#everything(String)} would be wrong here twice over: it hands out the
     * <em>query</em> language's built-ins, none of which a policy has, and it would say "all of them"
     * about an open set that has no such thing.</p>
     */
    private static final Capabilities EVERYTHING = Capabilities.of("jmp", PolicyCapability.every());

    /** The full destination — what {@link PolicyWriter} and a revision store write through. */
    public static final PolicySourceTranslator INSTANCE = new PolicySourceTranslator(EVERYTHING);

    private final Capabilities capabilities;

    private PolicySourceTranslator(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * A destination that writes only some of what a policy can hold.
     *
     * <p>⚠️ It does not render a subset — it <strong>refuses</strong> a document holding anything
     * else, which is the difference between a narrow destination and a lossy one. A documentation
     * screen showing the vocabulary a product is written against declares four of these and is told,
     * rather than quietly publishing a policy whose grants are missing.</p>
     *
     * <pre>{@code
     * PolicySourceTranslator.writing(PolicyCapability.SCOPES, PolicyCapability.PERMISSIONS);
     * }</pre>
     *
     * @param capabilities what it will write — from {@link PolicyCapability}
     * @return the narrowed destination
     */
    public static PolicySourceTranslator writing(Capability... capabilities) {
        return new PolicySourceTranslator(Capabilities.of("jmp", capabilities));
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    /**
     * Renders a policy, or one declaration of one, as source.
     *
     * @param node     a {@link PolicyNode}, or a single top-level declaration for a caller that wants
     *                 a fragment
     * @param bindings nothing — see the class note
     * @return the text, ready to be parsed back
     * @throws TranslationRefusedException where the tree is not a policy, holds a declaration a policy
     *                                     cannot hold, or came with bindings
     */
    @Override
    public String translate(Node node, Bindings bindings) {
        if (!bindings.isEmpty()) {
            throw new TranslationRefusedException(
                    ("the 'jmp' translator supplies nothing by name; a policy reads its variables from "
                     + "the decision it runs inside, and these would be silently dropped: %s")
                            .formatted(bindings.names()));
        }

        return requireWritable(node).toSource();
    }

    /**
     * Checks the tree against what this destination declares, before a character is written, and
     * hands back the node that will do the writing.
     *
     * <p>A whole policy is checked one declaration at a time; anything else is taken as the single
     * declaration a caller wants rendered on its own.</p>
     *
     * <p>⚠️ The seam takes a {@link Node} and un-parsing lives on {@link Expression} — a node that is
     * not an expression has no source form at all, so that is where the refusal belongs rather than
     * at a cast somebody hopes holds.</p>
     *
     * @param node what is about to be translated
     * @return the same node, as the expression that can render itself
     */
    private Expression requireWritable(Node node) {
        if (node instanceof PolicyNode policy) {
            for (Expression expression : policy.getExpressions()) {
                require(expression);
            }

            return policy;
        }

        if (node instanceof Expression expression) {
            require(expression);

            return expression;
        }

        throw new TranslationRefusedException(
                "the 'jmp' translator writes policy declarations, and %s is not one"
                        .formatted(node.getClass().getSimpleName()));
    }

    /**
     * Checks one top-level declaration.
     *
     * <p>⚠️ A node below that level — one grant, one scope line — is refused rather than rendered.
     * The capabilities are declared per declaration, so there is nothing for a destination to have
     * <em>said</em> about a fragment smaller than one, and rendering what was never declared is the
     * failure {@link Capabilities} exists to prevent.</p>
     *
     * @param expression a child of a policy
     */
    private void require(Expression expression) {
        Capability capability = PolicyCapability.of(expression);

        if (capability == null) {
            throw new TranslationRefusedException(
                    ("the 'jmp' translator cannot write '%s'; a policy holds 'include', 'scopes', "
                     + "'permissions', 'actions', 'variables', 'capabilities', 'role', 'plans', "
                     + "'subject' and 'entitlements' declarations")
                            .formatted(expression.getClass().getSimpleName()));
        }

        capabilities().require(capability, PolicyCapability.keyword(capability));
    }
}
