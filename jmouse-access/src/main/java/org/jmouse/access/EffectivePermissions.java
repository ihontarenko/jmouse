package org.jmouse.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What a subject may do at one target — and, for every permission, how it got that way.
 *
 * <pre>
 * effective(subject, target) =
 *       ⋃ { role bundles of every assignment whose scope covers target }
 *     ∪ { ALLOW overrides whose scope covers target }
 *     − { DENY  overrides whose scope covers target }
 * </pre>
 *
 * <p><strong>Deny wins, and the subtraction runs last, across every level.</strong> Deliberately the
 * same sentence as {@code Grant} (ADR-0013) and the same as the effective set that came before scopes
 * existed, so the installation has one rule to learn rather than three.
 *
 * <p><strong>Most-specific deliberately does not win.</strong> A personal deny at the installation
 * still takes a permission away from a workspace owner, which is exactly the property
 * {@code space:module:restrict} was invented to have — withholding is only withholding if the
 * workspace's own administrator cannot undo it.
 *
 * <p>Held-ness and provenance are one object rather than two computations. The control room renders
 * this; a control room that computes its own answer is a control room that lies on the day it
 * matters.
 */
public record EffectivePermissions(
        Map<String, PermissionProvenance> byPermission,
        Set<ScopeReference>               coveredScopes
) {

    private static final EffectivePermissions NOTHING = new EffectivePermissions(Map.of(), Set.of());

    public EffectivePermissions {
        byPermission  = Map.copyOf(byPermission);
        coveredScopes = Set.copyOf(coveredScopes);
    }

    /** Nobody, or a subject with no assignment covering the target at all. */
    public static EffectivePermissions none() {
        return NOTHING;
    }

    public boolean contains(String permission) {
        PermissionProvenance provenance = byPermission.get(permission);
        return provenance != null && provenance.held();
    }

    /**
     * Held, and <strong>nothing left that could still take it away</strong> once there is a row to
     * ask about.
     *
     * <p>⚠️ <strong>For the checks that are about protection rather than about permission.</strong>
     * {@link #contains} is the right reading almost everywhere: a conditionally granted permission
     * <em>is</em> held, and the condition narrows the particular request later. But a guard asking
     * <em>"would anybody still be able to do this after I save?"</em> is asking something else, and
     * {@code contains} answers it wrongly in the one direction that matters — it counts a route that
     * may never hold as a live holder.
     *
     * <p>The concrete failure: save a policy whose only remaining route to {@code access:policy:write}
     * is conditional, the rehearsal reports somebody still holds it, the condition fails at request
     * time, and the editor is shut to everybody — recoverable only through a database session.
     *
     * <p>Stated here rather than at the call site on purpose. A product writing
     * {@code held() && !isConditional()} inline is a product re-deriving an authorization rule, and
     * the second place it gets written is where the two spellings drift.
     */
    public boolean holdsUnconditionally(String permission) {
        PermissionProvenance provenance = byPermission.get(permission);
        return provenance != null && provenance.held() && !provenance.isConditional();
    }

    /**
     * Every route that granted this permission but might still not hold — empty where it is held
     * outright or not at all.
     *
     * <p>What a refusal quotes so that "nobody would still hold it" does not read as a lie to somebody
     * looking straight at a grant in the file.
     */
    public List<PermissionSource> conditionalRoutesTo(String permission) {
        PermissionProvenance provenance = byPermission.get(permission);

        if (provenance == null || holdsUnconditionally(permission)) {
            return List.of();
        }

        return provenance.grantedBy().stream().filter(PermissionSource::isConditional).toList();
    }

    /** The permissions actually held, sorted so two renderings of one answer read alike. */
    public Set<String> names() {
        return byPermission.entrySet().stream()
                .filter(entry -> entry.getValue().held())
                .map(Map.Entry::getKey)
                .collect(TreeSet::new, Set::add, Set::addAll);
    }

    /**
     * Everything this subject was ever routed, held or not.
     *
     * <p>A permission a deny removed still has an entry, because "you never had it" and "somebody
     * took it away, and here is who and why" are the two answers the <em>Who</em> view exists to tell
     * apart.
     */
    public Collection<PermissionProvenance> all() {
        return byPermission.values();
    }

    public PermissionProvenance provenanceOf(String permission) {
        return byPermission.get(permission);
    }

    /**
     * Whether anything at all reaches this subject at the given scope.
     *
     * <p>This is what "you are not a member" became: no assignment covering a workspace means no
     * permissions there, so the absence is not a check somebody has to remember to write — it is the
     * default answer, and it is the difference between telling a reader to ask for a permission and
     * telling them to ask to be added.
     */
    public boolean reaches(ScopeReference scope) {
        return coveredScopes.contains(scope);
    }

    /**
     * Capped against a master's set, which is what a service sub-account's effective set is.
     *
     * <p>Strictly tighter and never looser: the master must hold the permission <em>in the same
     * scope</em>. Applied per request rather than frozen when the agent was created, so a master that
     * loses a workspace loses it for its agents in the same request — which is what the hand-written
     * membership half of the cap was there to guarantee (see ADR-0006).
     */
    public EffectivePermissions cappedBy(EffectivePermissions master, ScopeReference at) {
        Map<String, PermissionProvenance> capped = new LinkedHashMap<>();

        byPermission.forEach((permission, provenance) -> capped.put(
                permission,
                provenance.held() && !master.contains(permission)
                        ? provenance.removedBy(PermissionSource.agentCap(at))
                        : provenance));

        // The agent reaches a scope only where its master does too — the same cap, one level up.
        Set<ScopeReference> reachable = new TreeSet<>(SCOPE_ORDER);
        coveredScopes.stream().filter(master.coveredScopes()::contains).forEach(reachable::add);

        return new EffectivePermissions(capped, reachable);
    }

    private static final Comparator<ScopeReference> SCOPE_ORDER =
            ScopeReference.WIDEST_FIRST.thenComparing(ScopeReference::id);

    /** Collects grants and denials into the resolved answer, with deny applied last. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, List<PermissionSource>> grants     = new TreeMap<>();
        private final Map<String, List<PermissionSource>> removals   = new TreeMap<>();
        private final Map<String, List<PermissionSource>> narrowings = new TreeMap<>();
        private final Set<ScopeReference>                 reached    = new TreeSet<>(SCOPE_ORDER);

        public Builder granted(String permission, PermissionSource source) {
            grants.computeIfAbsent(permission, key -> new ArrayList<>()).add(source);
            reached.add(source.scope());
            return this;
        }

        public Builder removed(String permission, PermissionSource source) {
            removals.computeIfAbsent(permission, key -> new ArrayList<>()).add(source);
            reached.add(source.scope());
            return this;
        }

        /**
         * A denial that has not happened, and might once there is a row to ask about.
         *
         * <p>⚠️ Kept apart from {@link #removed} on purpose, and it is the whole reason a conditional
         * denial can exist at all. Recorded as a removal it would take the permission away
         * <strong>unconditionally</strong> — the exact opposite of the rule somebody wrote — and
         * evaluating it later could not put it back, because the set is resolved once and the row is
         * not known yet.
         */
        public Builder narrowed(String permission, PermissionSource source) {
            narrowings.computeIfAbsent(permission, key -> new ArrayList<>()).add(source);
            reached.add(source.scope());
            return this;
        }

        /**
         * A scope this subject is present at even though nothing there granted a permission.
         *
         * <p>An assignment of an empty role is still an assignment: the holder is <em>in</em> the
         * workspace, and telling them to ask to be added would send them to somebody who would find
         * them already there.
         */
        public Builder reaches(ScopeReference scope) {
            reached.add(scope);
            return this;
        }

        public EffectivePermissions build() {
            Map<String, PermissionProvenance> resolved = new LinkedHashMap<>();

            TreeSet<String> mentioned = new TreeSet<>(grants.keySet());
            mentioned.addAll(removals.keySet());
            mentioned.addAll(narrowings.keySet());

            for (String permission : mentioned) {
                List<PermissionSource> grantedBy  = grants.getOrDefault(permission, List.of());
                List<PermissionSource> removedBy  = removals.getOrDefault(permission, List.of());
                List<PermissionSource> narrowedBy = narrowings.getOrDefault(permission, List.of());

                resolved.put(permission, new PermissionProvenance(
                        permission,
                        // A narrowing does not decide held-ness. It cannot: whether it applies is a
                        // question about a row, and this answer is resolved once for a whole page of
                        // them. Deny still wins here; a conditional deny wins later or not at all.
                        !grantedBy.isEmpty() && removedBy.isEmpty(),
                        List.copyOf(grantedBy),
                        List.copyOf(removedBy),
                        List.copyOf(narrowedBy)));
            }

            return new EffectivePermissions(resolved, reached);
        }
    }
}
