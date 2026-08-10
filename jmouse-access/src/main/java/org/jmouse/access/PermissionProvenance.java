package org.jmouse.access;

import java.util.ArrayList;
import java.util.List;

/**
 * One permission, whether the subject holds it, and every route that decided so.
 *
 * <p>Deny is carried as <strong>the thing that removed it</strong> rather than as an absence. That is
 * the whole point of deny winning last: somebody has to be able to find out why a power vanished, and
 * a permission that is simply missing from a list cannot tell them.
 *
 * @param permission the permission name
 * @param held       whether it survives the subtraction
 * @param grantedBy  every assignment or allow that routed it here, empty where nothing did
 * @param removedBy  every deny that took it away, empty where none did
 * @param narrowedBy ⚠️ every <strong>conditional deny</strong> — a denial that has not happened and
 *                   might. Deliberately not in {@link #removedBy}: applied there it would take the
 *                   permission away unconditionally, which is the opposite of what the rule says. It
 *                   is carried so an axis running after the permission axis can subtract with it once
 *                   there is a row to ask about
 */
public record PermissionProvenance(
        String                 permission,
        boolean                held,
        List<PermissionSource> grantedBy,
        List<PermissionSource> removedBy,
        List<PermissionSource> narrowedBy
) {

    public PermissionProvenance {
        grantedBy  = List.copyOf(grantedBy);
        removedBy  = List.copyOf(removedBy);
        narrowedBy = narrowedBy == null ? List.of() : List.copyOf(narrowedBy);
    }

    /** An unconditional reading, for whoever has no conditions to record. */
    public PermissionProvenance(
            String                 permission,
            boolean                held,
            List<PermissionSource> grantedBy,
            List<PermissionSource> removedBy) {

        this(permission, held, grantedBy, removedBy, List.of());
    }

    /** The same permission, taken away by one more thing — how the agent cap is recorded. */
    public PermissionProvenance removedBy(PermissionSource source) {
        List<PermissionSource> removals = new ArrayList<>(removedBy);
        removals.add(source);

        return new PermissionProvenance(permission, false, grantedBy, removals, narrowedBy);
    }

    /**
     * Whether anything could still take this away once there is a row to ask about.
     *
     * <p>True in two cases, and they are different rules with the same consequence: a conditional
     * deny is waiting, or <strong>every</strong> route that granted it is itself conditional — in
     * which case the set holds it and a particular row may still be refused.
     */
    public boolean isConditional() {
        return !narrowedBy.isEmpty()
               || (!grantedBy.isEmpty() && grantedBy.stream().allMatch(PermissionSource::isConditional));
    }

    /**
     * Held once and taken away since — as opposed to never routed here at all.
     *
     * <p>The distinction the <em>Who</em> view renders differently: one is a permission somebody
     * removed on purpose, the other is one nobody ever gave.
     */
    public boolean wasRemoved() {
        return !held && !grantedBy.isEmpty();
    }
}
