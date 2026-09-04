package org.jmouse.access.jpa;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.BundledPermission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The question the runtime never asks: <em>who holds this</em>, rather than <em>does this person
 * hold it</em>.
 *
 * <h2>⚠️ This is a disclosure surface, and it is a third port on purpose</h2>
 *
 * <p>{@code GrantStore} is the engine's and answers about <strong>one</strong> subject, because
 * <em>"a store that could list every account would be a store the engine could walk"</em>.
 * {@link AccessAdministration} writes, and lists roles — a bounded vocabulary — and nothing else. Both
 * refusals are deliberate and neither is relaxed here.
 *
 * <p>What is also true is that an administration screen genuinely has to answer <em>"who can do this,
 * and who took it away from them"</em>, and a rehearsal has to know whom a candidate policy could
 * possibly affect. Those are real needs, they are unbounded by nature, and the honest response is a
 * separate interface that says so in its name — <strong>not</strong> a method quietly added to one of
 * the other two, where the engine would then be holding a store it could walk.
 *
 * <p>⚠️ <strong>Nothing on the decision path may take one of these.</strong> A product exposing it puts
 * it behind a permission of its own and records the reading, which is what the word <em>disclosure</em>
 * is here to keep in mind. It enumerates <em>grants</em> rather than accounts — an account that holds
 * nothing never appears — but every account that holds anything does, which is the whole installation
 * for any product where holding something is what having an account means.
 */
public interface AccessDisclosure {

    /**
     * Every role anybody holds anywhere, each with the bundle its role carries.
     *
     * <p>⚠️ The bundle is <strong>unioned with what a policy document declares</strong>, exactly as the
     * engine's own store unions it. A screen reading only the table would answer "nobody" for every
     * permission the declared roles carry — which is most of them, and the one answer a screen built to
     * explain authorization must never give wrongly.
     */
    List<RoleHolding> roleHoldings();

    /** Every personal allow and deny anybody holds anywhere. */
    List<DirectHolding> directHoldings();

    /** The same, narrowed to one permission — what a screen about a single power asks. */
    List<DirectHolding> directHoldingsOf(String permission);

    /**
     * How many subjects hold one role at one place — a number, never a list.
     *
     * <p>⚠️ This is the <em>last owner</em> guard, and it is the one question about holders that
     * discloses nothing: a workspace whose only owner is about to leave has no way back that does not
     * involve somebody with installation-wide access, and the whole point of a workspace is that it
     * does not need one.
     *
     * <p>⚠️ It counts <strong>rows</strong>. A holder a policy document names is not in the answer,
     * which is correct here: a guard about whether a revocation would strand a place must not be
     * satisfied by a holder no revocation could produce.
     */
    long holdersOf(String roleName, ScopeReference at);

    /**
     * One subject's role, where they hold it, and what it carries.
     *
     * @param at     where the <em>assignment</em> was made, which is never the bundle's own reach
     * @param reason why it was handed over, in words, or null. ⚠️ <strong>The same field, and the same
     *               argument, as {@link DirectHolding#reason()}</strong> — and it was missing here for
     *               a long time while the grammar, the policy model and the writer all carried it. A
     *               projection built without it re-rendered every document with the sentence stripped
     *               out, which is worse than never having offered one: the editor opens on that
     *               projection and writes back what it was shown
     */
    record RoleHolding(
            String                  subjectId,
            String                  roleName,
            ScopeReference          at,
            String                  grantedBy,
            LocalDateTime           since,
            String                  condition,
            String                  reason,
            List<BundledPermission> bundle
    ) {

        /** ⚠️ Kept for callers that predate conditions. A holding with none is the ordinary one. */
        public RoleHolding(String subjectId, String roleName, ScopeReference at, String grantedBy,
                           LocalDateTime since, List<BundledPermission> bundle) {

            this(subjectId, roleName, at, grantedBy, since, null, null, bundle);
        }

        /**
         * ⚠️ Kept for callers that predate reasons, so adding one touched no construction site.
         *
         * <p>Note what the sibling record teaches: {@code DirectHolding} grew its arity-preserving
         * constructor for the condition and a caller quietly went on using it, which is how a reason
         * reached a screen as null for months. An overload is a convenience, never a default worth
         * reaching for — if a caller <em>knows</em> the reason, it passes it.</p>
         */
        public RoleHolding(String subjectId, String roleName, ScopeReference at, String grantedBy,
                           LocalDateTime since, String condition, List<BundledPermission> bundle) {

            this(subjectId, roleName, at, grantedBy, since, condition, null, bundle);
        }

        /** Whether this assignment says, in words, why it is what it is. */
        public boolean isExplained() {
            return reason != null && !reason.isBlank();
        }

        /** How far this holding carries one permission, or null where it does not carry it at all. */
        public BundledPermission carrying(String permission) {
            return bundle.stream()
                    .filter(entry -> entry.permission().equals(permission))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * One subject's personal allow or deny.
     *
     * <p>⚠️ {@code reason} is what makes a deny answerable a year later, and it is the field the whole
     * record exists for: a permission somebody took away is not the same fact as one nobody ever gave.
     */
    record DirectHolding(
            String         subjectId,
            String         permission,
            boolean        allowed,
            ScopeReference at,
            String         grantedBy,
            String         reason,
            LocalDateTime  since,
            String         condition
    ) {

        /**
         * ⚠️ <strong>The condition is source text and it has to be here.</strong> This record is what a
         * screen renders and what a projection writes back out as a document — and a projection that
         * dropped conditions would describe rows that grant more than they do, which anything reading
         * that description back would then apply.
         */
        public DirectHolding(String subjectId, String permission, boolean allowed, ScopeReference at,
                             String grantedBy, String reason, LocalDateTime since) {

            this(subjectId, permission, allowed, at, grantedBy, reason, since, null);
        }
    }
}
