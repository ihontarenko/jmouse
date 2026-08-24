package org.jmouse.access.spi;

import org.jmouse.access.Subject;

/**
 * Whether a subject owns a thing — the port behind {@code resource is ownedBy(caller)}.
 *
 * <p><em>"Your own rows"</em> is the most common thing an authorization rule wants to say, and until
 * now there was no way to say it in a policy file. Every product answered it in Java instead,
 * differently, once per call site.
 *
 * <h2>⚠️ "Owns" has two honest answers, and a product will pick the wrong one by accident</h2>
 *
 * <p>An <strong>agent</strong> acts for its master. An administrator <strong>working as</strong>
 * somebody acts for them. So <em>does this caller own it</em> can mean:
 *
 * <ol>
 *   <li><strong>owner of record</strong> — is the row's owner literally this principal;</li>
 *   <li><strong>acting for the owner</strong> — is the row owned by whoever this caller is acting as.</li>
 * </ol>
 *
 * <p>⚠️ <strong>This port is the second one.</strong> An agent filing a report on its master's row is
 * doing the master's work, and a rule saying <em>"you may edit your own"</em> means the person, not the
 * credential. {@link Subject} carries {@link Subject#masterId()} and {@link Subject#originId()} for
 * exactly this, and an implementation that compares only {@link Subject#principalId()} answers the first
 * question while looking like it answers the second.
 *
 * <p>An implementation that genuinely wants owner-of-record should say so in its own javadoc, and should
 * expect a rule written against it to behave differently for an agent than for the person it acts for.
 *
 * <h2>⚠️ Answer from what you were handed, or say you cannot</h2>
 *
 * <p>{@code ConditionAxis} already has the resource in hand, so this costs a field read — <em>provided
 * the implementation does not go looking</em>. A resolver that fetches is a query per decision on the
 * last axis of every request.
 *
 * <p>A resolver that cannot answer should <strong>throw</strong> rather than return {@code false}: a
 * {@code false} inside a {@code deny} permits, and <em>"I could not tell whether you own this"</em> is
 * not <em>"you do not"</em>.
 */
public interface OwnershipResolver {

    /**
     * @param subject  who is acting — see the class javadoc on which of the two questions this is
     * @param resource what the axis was handed for this target; may be {@code null}
     * @return whether the subject owns it
     */
    boolean owns(Subject subject, Object resource);

    /**
     * The installation that owns nothing — what an unimplemented port answers.
     *
     * <p>It answers {@code false}, and ⚠️ <strong>that is fail-closed in both positions</strong>, which
     * is why it is a plain answer here rather than a throw:
     *
     * <table>
     *   <tr><td>{@code allow x when resource is ownedBy(caller)}</td>
     *       <td>{@code false} → the allow is dropped → <strong>refuse</strong></td></tr>
     *   <tr><td>{@code deny x when resource is not ownedBy(caller)}</td>
     *       <td>{@code false} → {@code is not} holds → the deny applies → <strong>refuse</strong></td></tr>
     * </table>
     *
     * <p>So an installation that has not wired ownership refuses rather than waves through, in either
     * spelling. It will look strict rather than broken, which is the correct way round.
     */
    static OwnershipResolver nothing() {
        return (subject, resource) -> false;
    }
}
