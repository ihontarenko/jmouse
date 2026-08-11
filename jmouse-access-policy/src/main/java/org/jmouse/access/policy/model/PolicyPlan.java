package org.jmouse.access.policy.model;

import java.util.List;

/**
 * A named bundle of qualified capability grants — what {@link PolicyRole} is for permissions.
 *
 * <pre>
 *   role : permission  ::  plan : capability
 * </pre>
 *
 * <p>That symmetry is the entire reason the word carries no money. A plan here is a name and a set of
 * lines; whether the name meant a subscription, an internal allocation or a pilot programme is
 * provenance recorded on the grants it issues, and nothing in this module needs to know which.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * plans {
 *     plan business "Business" order 30 note "Everything the product has." {
 *         seat          unlimited
 *         storage-byte  100GB per month
 *         parametric-search
 *     }
 *
 *     plan enterprise "Enterprise" order 40 extends business {
 *         storage-byte  unlimited
 *     }
 * }
 * </pre>
 *
 * <h2>⚠️ Metadata is in the header, capabilities are in the body</h2>
 *
 * <p>A body holding both {@code note "…"} and {@code seat 5} would make the grammar depend on the
 * product's catalogue never containing a capability called {@code note} or {@code order}. Every line
 * in the body is the same kind of line, so a parser never has to decide what a bare word meant.
 *
 * <h2>⚠️ A plan may only ever give</h2>
 *
 * <p>There is no deny here and there must not be. Taking something away from one customer is an act
 * with a person and a reason behind it, and a catalogue entry carries neither — that is a deny in
 * {@code entitlements { }}, where it can be attributed and explained.
 *
 * @param code        what the product calls this tier — arbitrary, and no more reserved than a role
 *                    name is
 * @param displayName what a screen shows, or null
 * @param order       display order from {@code order N}, or 0
 * @param note        the sentence from {@code note "…"}, or null
 * @param extendsCode the bundle this one starts from, or null. A line restated here overrides the
 *                    inherited one — without it, "the same, without the ceilings" is a copy that
 *                    drifts from its original at the first edit
 * @param grants      the body, in the order written
 */
public record PolicyPlan(
        String               code,
        String               displayName,
        int                  order,
        String               note,
        String               extendsCode,
        List<PolicyPlanGrant> grants,
        SourceSpan           at
) {

    public PolicyPlan {
        grants = grants == null ? List.of() : List.copyOf(grants);
    }

    public boolean isDerived() {
        return extendsCode != null && !extendsCode.isBlank();
    }
}
