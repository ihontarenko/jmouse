package org.jmouse.access.policy;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;

import java.util.List;

/**
 * The policy currently in force — and the seam that lets it be replaced while the application runs.
 *
 * <pre>
 *   .jmp text ──parse──► PolicyDocument ──bind──► AccessPolicy ──► PolicyGrantStore
 *                                          │                            │
 *                                     rehearse()                    the engine reads
 *                                    (bind, discard)                    this, always
 *                                          │                            ▲
 *                                       adopt() ─────── swap ───────────┘
 * </pre>
 *
 * <h2>Why this exists at all</h2>
 *
 * <p>{@link PolicyGrantStore} is immutable by construction, which is right: a store that could be
 * edited under a request would answer two different things inside one decision. But an application
 * whose policy is only ever built at startup has no way to change authorization without a deploy, and
 * a screen that needs a deploy to take effect is a screen people work around.
 *
 * <p>So the store stays immutable and <strong>the reference moves</strong>. Every read here goes
 * through one {@code volatile} field, so a request either sees the whole previous revision or the
 * whole next one, and never a mixture of the two.
 *
 * <h2>⚠️ Bind first, swap second — a failure must change nothing</h2>
 *
 * <p>{@link #adopt(PolicyDocument)} binds the candidate <em>before</em> touching the field, so a
 * document naming an unregistered scope, a permission nobody declared, or a condition that will not
 * compile throws with the field untouched. A half-applied policy is an installation whose permissions
 * are neither what the file says nor what it said yesterday, and there is no log line that makes that
 * better than a refusal.
 *
 * <p>{@link #rehearse(PolicyDocument)} is the same work with the swap left out: it answers <em>would
 * this hold, and what would it mean</em> without anybody's permissions changing. Deny-wins and bundle
 * inheritance make the consequences of a two-line edit genuinely hard to predict, which is both the
 * argument for a dry run and the argument for the guard.
 */
public final class LivePolicy implements GrantStore {

    private final PolicyBinder binder;

    /**
     * ⚠️ The one mutable field in the whole feature, and the only reason it is safe: a reader sees
     * one revision or the other, because a reference assignment cannot be observed half-done.
     */
    private volatile Revision current;

    /**
     * @param binder   how a document becomes engine types — the vocabulary this installation registered
     * @param document what is in force at startup, already bound once here so a broken file stops the
     *                 context rather than the first request
     */
    public LivePolicy(PolicyBinder binder, PolicyDocument document) {
        this.binder  = binder;
        this.current = revisionOf(binder, document);
    }

    /** What the policy currently in force was written as. */
    public PolicyDocument document() {
        return current.document();
    }

    /** The same policy bound — names resolved, wildcards expanded, every scope a registered one. */
    public AccessPolicy policy() {
        return current.policy();
    }

    /**
     * Binds a candidate and throws it away, so a caller can ask what it would mean.
     *
     * @param candidate the document being considered
     * @return the policy it would become
     * @throws PolicyException where it would not bind, carrying every problem with its line
     */
    public AccessPolicy rehearse(PolicyDocument candidate) {
        return binder.bind(candidate);
    }

    /**
     * Puts a candidate in force, or leaves everything exactly as it was.
     *
     * @param candidate the document to adopt
     * @return the policy now in force
     * @throws PolicyException where it would not bind — and nothing has changed
     */
    public AccessPolicy adopt(PolicyDocument candidate) {
        Revision next = revisionOf(binder, candidate);

        current = next;

        return next.policy();
    }

    // ── GrantStore, over whichever revision is in force ───────────────────────
    //
    // Each answer reads the field exactly once, so it is drawn whole from one revision. The engine
    // asks four of these per decision and a swap may land between two of them — which is deliberate
    // and harmless, because both revisions are complete policies and deny still wins within each.

    @Override
    public List<RoleGrant> rolesCovering(String subjectId, List<ScopeReference> chain) {
        return current.store().rolesCovering(subjectId, chain);
    }

    @Override
    public List<DirectGrant> directCovering(String subjectId, List<ScopeReference> chain) {
        return current.store().directCovering(subjectId, chain);
    }

    @Override
    public List<RoleGrant> rolesHeldBy(String subjectId) {
        return current.store().rolesHeldBy(subjectId);
    }

    @Override
    public List<DirectGrant> directHeldBy(String subjectId) {
        return current.store().directHeldBy(subjectId);
    }

    private static Revision revisionOf(PolicyBinder binder, PolicyDocument document) {
        AccessPolicy policy = binder.bind(document);

        return new Revision(document, policy, new PolicyGrantStore(policy));
    }

    /**
     * One policy in all three of its readings, kept together so they cannot disagree.
     *
     * <p>Swapping a document without its store, or a store without the text it came from, is how a
     * control room comes to show one policy while the engine answers from another.
     */
    private record Revision(PolicyDocument document, AccessPolicy policy, PolicyGrantStore store) {
    }
}
