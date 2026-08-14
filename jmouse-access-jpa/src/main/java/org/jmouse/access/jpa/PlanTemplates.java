package org.jmouse.access.jpa;

import java.util.List;
import java.util.Optional;

/**
 * The tier catalogue — <strong>which tiers exist and what each one includes</strong>.
 *
 * <h2>⚠️ A catalogue of templates, and nothing more</h2>
 *
 * <p>Nothing here answers <em>what may this account do</em>. Who is on a tier is a capability grant
 * carrying the tier as its {@code source_reference}, and that is the only place it is recorded. This
 * port exists because <em>what Business contains</em> had to live somewhere once a policy document
 * stopped being read at runtime — and it is read at the moment somebody is <em>put</em> on a tier,
 * never on the path of a decision.
 *
 * <p>⚠️ It does not resurrect the {@code plans} table that was deleted. That one was half of a fact
 * written twice: the grants said who was on a tier and a column said it again. A template duplicates
 * nothing, because no row points at it as an answer. The similar name is precisely why this paragraph
 * is here.
 *
 * <h2>⚠️ {@code extends} is resolved by the caller, not here</h2>
 *
 * <p>A tier may be written as a difference from another and this port answers with the difference,
 * exactly as stored. Resolving a lineage means knowing what an amount <em>means</em> — {@code 100GB}
 * against {@code 25} — which is the product's knowledge and not a schema's. An editor also wants the
 * unresolved form, so that an inherited line can be shown as inherited.
 */
public interface PlanTemplates {

    /** Every tier, cheapest first by declared order. A bounded catalogue, so listing it is safe. */
    List<TierView> all();

    Optional<TierView> byCode(String code);

    /**
     * Brings a tier into being or edits the one that is there, with exactly what it includes.
     *
     * <p>⚠️ <strong>This does not reissue anything.</strong> A grant is materialised from a template at
     * the moment of assignment, so editing a tier changes what the next account put on it receives and
     * nothing about the accounts already on it. That is deliberate — a text edit that silently
     * re-provisioned every paying customer would be an unreviewable mass mutation — and any screen
     * calling this has to say so.
     *
     * @return whether anything actually changed, so a caller can audit an edit and ignore a no-op
     */
    boolean define(TierView tier);

    /**
     * Removes a tier from the catalogue.
     *
     * <p>⚠️ Accounts already on it <strong>keep what they were given</strong>: their grants are rows
     * and this deletes a template. What stops working is putting anybody else on it.
     */
    boolean retire(String code);

    /**
     * @param extendsCode the tier this one is written as a difference from, or null
     * @param includes    what it contains, as written — see {@link Inclusion} for why the amount is text
     */
    record TierView(
            String         code,
            String         displayName,
            int            order,
            String         note,
            String         extendsCode,
            List<Inclusion> includes
    ) {
    }

    /**
     * @param quantity  the amount exactly as somebody wrote it — {@code 100GB}, {@code 25} — or null.
     *                  ⚠️ Text, because what a unit means belongs to the product; a column parsing it
     *                  would make this schema hold an opinion about units it cannot have
     * @param unlimited whether the line says so. ⚠️ Not the same as a null quantity: no amount and no
     *                  {@code unlimited} is a <em>gate</em>, included and never counted
     */
    record Inclusion(String capability, String quantity, String period, boolean unlimited) {
    }
}
