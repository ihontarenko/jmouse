package org.jmouse.access.spi;

import java.time.LocalDateTime;

/**
 * What a rule was written with, beyond its own subject matter — who recorded it, where the rule
 * itself lives, and what narrows it beyond the scope it applies at.
 *
 * <p>These five facts used to be repeated as five components on {@link DirectGrant}, three on
 * {@link RoleGrant} and five again on {@link org.jmouse.access.PermissionSource}, which is one idea
 * wearing three shapes. They are always read together — the control room renders them as one block
 * and a refusal quotes them as one sentence — so they travel as one value.
 *
 * <p>⚠️ <strong>It is the reason a new fact about a rule costs nothing.</strong> Attaching a
 * condition to a role assignment or to a bundle entry would otherwise mean one more component on
 * every record that carries a grant, plus one more compatibility overload on each to keep existing
 * stores compiling. Here it is a field on a value everybody already holds.
 *
 * @param grantedBy who recorded this, where anybody did. Null for a rule nobody handed out — a
 *                  ceiling, a share link
 * @param reason    what they typed at the time — the field that exists so a vanished power can be
 *                  explained eight months later, when asking the person who did it is not an answer
 * @param since     when it was written, where that was recorded
 * @param origin    ⚠️ whether a <strong>row or a line</strong> holds the rule. How a permission
 *                  arrived and where it is written down are two questions, and only the second
 *                  answers <em>"may this screen change it"</em>
 * @param condition ⚠️ <strong>carried, never evaluated during resolution</strong>, or null for the
 *                  ordinary unconditional rule. The effective set stays row-independent and
 *                  cacheable; the condition is read afterwards by an axis that may only narrow. See
 *                  {@link GrantCondition}
 */
public record GrantAttribution(
        String         grantedBy,
        String         reason,
        LocalDateTime  since,
        GrantOrigin    origin,
        GrantCondition condition
) {

    private static final GrantAttribution NOBODY = new GrantAttribution(null, null, null, null, null);

    public GrantAttribution {
        origin = origin == null ? GrantOrigin.stored() : origin;
    }

    /**
     * A rule a table holds — the ordinary case, and what a store that knows nothing of origins or
     * conditions answers with.
     */
    public static GrantAttribution stored(String grantedBy, LocalDateTime since) {
        return new GrantAttribution(grantedBy, null, since, GrantOrigin.stored(), null);
    }

    /** The same, with the words somebody typed at the time. */
    public static GrantAttribution stored(String grantedBy, String reason, LocalDateTime since) {
        return new GrantAttribution(grantedBy, reason, since, GrantOrigin.stored(), null);
    }

    /**
     * A rule a policy document holds, at a line somebody can open.
     *
     * @param grantedBy what recorded it — conventionally {@code policy:<name>}
     * @param reason    where in the document, in words a reader can act on
     * @param document  what the file is called
     * @param line      which line of it
     */
    public static GrantAttribution declaredIn(
            String grantedBy, String reason, String document, int line) {

        return new GrantAttribution(
                grantedBy, reason, LocalDateTime.now(), GrantOrigin.declaredIn(document, line), null);
    }

    /**
     * A rule nobody handed out: a ceiling, a share link, anything the engine derives rather than
     * reads.
     *
     * <p>⚠️ {@link #grantedBy()} is null on purpose here rather than "system". A control room that
     * printed a name would be inviting somebody to go and ask them.
     */
    public static GrantAttribution derived(String reason) {
        return new GrantAttribution(null, reason, null, GrantOrigin.stored(), null);
    }

    /** Nothing recorded at all. */
    public static GrantAttribution none() {
        return NOBODY;
    }

    /**
     * The same attribution, narrowed by one more condition.
     *
     * <p>Composed rather than replaced: an assignment's condition and a bundle entry's condition both
     * apply, and a narrowing composed of narrowings is still a narrowing. See
     * {@link GrantCondition#all}.
     */
    public GrantAttribution narrowedBy(GrantCondition narrowing) {
        return narrowing == null
                ? this
                : new GrantAttribution(
                        grantedBy, reason, since, origin, GrantCondition.all(condition, narrowing));
    }

    /** Whether anything narrows the rule beyond the scope it applies at. */
    public boolean isConditional() {
        return condition != null;
    }

    /** Whether a file holds the rule rather than a table. */
    public boolean isDeclared() {
        return origin.isDeclared();
    }
}
