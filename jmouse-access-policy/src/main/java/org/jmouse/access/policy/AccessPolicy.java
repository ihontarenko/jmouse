package org.jmouse.access.policy;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.GrantAttribution;
import org.jmouse.access.spi.DirectGrant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * A policy document after binding — names resolved, permissions expanded, every scope a real
 * {@link org.jmouse.access.ScopeKind} this installation registered.
 *
 * <p>Where {@link org.jmouse.access.policy.model.PolicyDocument} is what a file <em>said</em>, this is
 * what it <em>means</em>. Everything questionable has already been questioned: binding either produced
 * this or threw, so nothing downstream validates anything.
 *
 * <p>It holds the engine's own types rather than parallel ones, which is the whole argument for the
 * feature — a policy is another way to write grants, not a second authorization model.
 *
 * @param name       the document's name, for provenance
 * @param roles      bundles by role name
 * @param subjects   what each subject holds, by subject identifier
 * @param loadedAt   when — every grant reports this as its {@code since}, because a grant with no
 *                   recorded origin reads as a defect to whoever is explaining a permission
 */
public record AccessPolicy(
        String                               name,
        Map<String, List<BundledPermission>> roles,
        Map<String, BoundSubject>            subjects,
        LocalDateTime                        loadedAt
) {

    public AccessPolicy {
        roles    = Map.copyOf(roles);
        subjects = Map.copyOf(subjects);
    }

    /** How a grant from this policy names its origin. */
    public String provenance() {
        return "policy:" + name;
    }

    /** One subject's holdings. */
    public record BoundSubject(List<BoundAssignment> roles, List<DirectGrant> grants) {

        public BoundSubject {
            roles  = List.copyOf(roles);
            grants = List.copyOf(grants);
        }

        public static BoundSubject empty() {
            return new BoundSubject(List.of(), List.of());
        }
    }

    /**
     * A role held at a scope. The bundle is looked up by name, so a role is stored once.
     *
     * @param attribution what the document said and where — which file, which line, and what narrows
     *                    <em>this handing-out</em>. The control room renders it, and the origin inside
     *                    it is what tells a screen not to offer a row editor for something no row
     *                    holds. ⚠️ A condition here belongs to the assignment, not to the role: the
     *                    same role assigned elsewhere is unaffected
     */
    public record BoundAssignment(String roleName, ScopeReference at, GrantAttribution attribution) {

        public BoundAssignment {
            attribution = attribution == null ? GrantAttribution.none() : attribution;
        }
    }
}
