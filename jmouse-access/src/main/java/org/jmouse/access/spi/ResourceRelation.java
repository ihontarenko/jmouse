package org.jmouse.access.spi;

import org.jmouse.access.AccessTarget;

import java.util.List;

/**
 * Where a row that has no place of its own borrows one — declared by the feature, walked by the engine.
 *
 * <h2>Why it exists</h2>
 *
 * <p>{@link AccessTargetResolver} answers <em>where does this row live</em>, and for most rows that is a
 * complete answer. For some it is not: a row can have no owner and no place, and still be perfectly
 * governable, because it hangs off a row that has both.
 *
 * <p>The case that raised this: a <em>field definition</em> has no owner column and belongs to no
 * workspace — one field stands on many forms — so every guard over it could only ask about the
 * workspace the request happened to name, which meant anybody holding the permission anywhere held it
 * everywhere. Measured in a live installation: of 283 fields, 230 stood on one form, twelve on between
 * eighteen and forty-five, and fifteen on none at all.
 *
 * <p>It is a <strong>category rather than a case</strong>. An option on a field, a comment on a page, a
 * movement on a position: each is a row whose governing place is somebody else's. Without this seam each
 * one grows a hand-written check inside a service, which is the drift {@code AccessTargetResolver} was
 * introduced to end — the same rule written twice, disagreeing on the third change.
 *
 * <h2>⚠️ This is the one thing in the model that can WIDEN</h2>
 *
 * <p>A condition may only narrow: {@code ConditionAxis} never grants. A relation does not narrow — it
 * <strong>moves where the check lands</strong>. So a wrong relation does not refuse too much, it
 * <em>permits</em> too much, which is the opposite failure mode from everything else here and the reason
 * for the two rules below.
 *
 * <ul>
 *   <li><strong>One hop.</strong> The far end's own {@code through} is not followed. A chain is a cycle
 *       waiting to be written, and a permission whose target is three rows away is a permission nobody
 *       can reason about.
 *   <li><strong>Empty falls to the installation.</strong> A row related to nothing has borrowed no
 *       place, so the engine asks about {@code AccessTarget.installation()} — which only a {@code GLOBAL}
 *       holding covers. It hands nobody a permission they did not have, since an installation-wide holder
 *       could already act on every row of the type; a holding at an organisation, a workspace or
 *       {@code SELF} does not cover it.
 *       <p>⚠️ This is <strong>not</strong> the same as returning an unscoped target, which would pass
 *       every axis that is about a place. And it is deliberately not a refusal: refusing was the first
 *       design, and fifteen of Innoventa's 283 fields stand on no form — under that rule nobody, an
 *       administrator included, could ever have edited or deleted one, with no way out from inside the
 *       product.
 * </ul>
 *
 * <h2>Why the policy names the TYPE and not this bean</h2>
 *
 * <p>A rule is written {@code field:write "…" through form} — and {@code form} is the resource kind from
 * {@link AccessTargetRegistry}, the same word {@code @RequiresAccess(resource = Form.class)} means. It is
 * not this relation's name and not a free string: a closed vocabulary is checked at load, and a reader of
 * the policy can see where a rule aims without opening any Java.
 *
 * <p>So a relation is keyed on the <strong>pair</strong> {@code (from, to)}. Two relations for one pair
 * is a startup failure, exactly as two resolvers for one type are — if a second path between the same two
 * types ever exists, the syntax grows a name for it rather than the registry guessing.
 *
 * @param <T> the type this relation leads away from
 */
public interface ResourceRelation<T> {

    /** The type a row must be for this relation to apply. */
    Class<T> from();

    /**
     * The type this relation leads to — and therefore the word a policy writes after {@code through}.
     *
     * <p>Whatever it names must be a type some {@link AccessTargetResolver} speaks for, or the
     * traversal lands on a row nothing can place. Checked at startup rather than discovered at a
     * refusal.
     */
    Class<?> to();

    /**
     * Every row of {@link #to()} this row hangs off, already resolved to targets.
     *
     * <p>⚠️ <strong>Resolved here rather than returned as identifiers</strong>, so that a relation may
     * answer in one query instead of handing back ids the engine then resolves one at a time — the N+1
     * this whole seam exists on the hot path to avoid.
     *
     * <p>⚠️ <strong>An empty list means refused.</strong> See the class note: it is not "no constraint".
     */
    List<AccessTarget> targetsOf(String resourceId);

    /**
     * The same answer for a page of rows.
     *
     * <p>The default is honest rather than fast and is fine for a resource nothing lists; override it
     * wherever a listing asks about many, for the reason {@link AccessTargetResolver#resolveAll} gives.
     */
    default java.util.Map<String, List<AccessTarget>> targetsOfAll(List<String> resourceIds) {
        return resourceIds.stream()
                .distinct()
                .collect(java.util.stream.Collectors.toMap(
                        resourceId -> resourceId,
                        this::targetsOf));
    }
}
