package org.jmouse.storage.policy;

/**
 * 🛃 Which acceptance policy applies to content going <strong>here</strong>.
 *
 * <p>{@link UploadPolicy} answers "may this content be stored"; this answers "by which rule". The two
 * were one thing for as long as an installation had a single answer, and separating them is what lets a
 * folder — or a project, or a workspace — carry its own.</p>
 *
 * <h3>⚠️ The argument is the destination, never the content</h3>
 *
 * <p>A policy is resolved before a byte is read, and the content is what it then judges. Handing this
 * method the {@link org.jmouse.storage.Content} would invite an implementation that decides per file,
 * which is a different feature and a worse one: a rule nobody can read off a screen, because it does not
 * exist until something is uploaded.</p>
 *
 * <h3>⚠️ Why two parameters rather than one {@code KIND:id} string</h3>
 *
 * <p>Because {@code OwnerReference} lives in {@code jmouse-files} and this module does not depend on it —
 * so the alternative was a joined string plus a separator constant defined a second time here, and an
 * implementation splitting it back apart on every upload. Two values that are already two values cost
 * nothing to pass and nothing to read. A caller holding an {@code OwnerReference} spreads it; an
 * implementation wanting one composes it.</p>
 *
 * <p>Both may be {@code null}: a caller that does not say where content is going is telling the truth
 * about not knowing, and every implementation must read that as "the installation's own policy".</p>
 *
 * @see FixedUploadPolicy the answer for an installation with one rule
 */
@FunctionalInterface
public interface UploadPolicyResolver {

    /**
     * 🛃 The policy governing what may enter this destination.
     *
     * @param ownerType what kind of thing will hold the content, or {@code null} when unknown
     * @param ownerId   which one, or {@code null} when unknown
     * @return the policy to judge the content by — never {@code null}
     */
    UploadPolicy policyFor(String ownerType, String ownerId);
}
