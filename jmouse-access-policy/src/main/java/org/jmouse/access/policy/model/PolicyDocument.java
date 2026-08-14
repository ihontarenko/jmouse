package org.jmouse.access.policy.model;

import java.util.List;
import java.util.Optional;

/**
 * One parsed policy file — <strong>the contract between a parser and the rest of this module</strong>.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * policy "example" {
 *     include 'bootstrap.jmp'
 *
 *     role SPACE_ADMIN {
 *         {@literal @}SPACE         space:write
 *         {@literal @}INSTALLATION  form:read
 *     }
 *
 *     subject u-42 {
 *         grants SPACE_ADMIN {@literal @}SPACE:kyiv
 *         {@literal @}SELF  form:write  deny
 *     }
 * }
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicyDocument("example",
 *       List.of(new PolicyInclude("bootstrap.jmp", new SourceSpan(2, 5))),
 *       List.of(new PolicyRole("SPACE_ADMIN", List.of(
 *               new PolicyBundleEntry("space:write", "SPACE",        new SourceSpan(5, 24)),
 *               new PolicyBundleEntry("form:read",   "INSTALLATION", new SourceSpan(6, 24))),
 *               new SourceSpan(4, 5))),
 *       List.of(new PolicySubject("u-42",
 *               List.of(new PolicyRoleAssignment("SPACE_ADMIN",
 *                           new PolicyScope("SPACE", "kyiv"), new SourceSpan(10, 9))),
 *               List.of(new PolicyGrant("form:write", new PolicyScope("SELF", null),
 *                           PolicyEffect.DENY, null, new SourceSpan(11, 9))),
 *               new SourceSpan(9, 5))))
 * </pre>
 *
 * <h2>Everything in here is text</h2>
 *
 * <p>No scope kinds, no permissions-as-types, no compiled expressions, no catalogue lookups: a parser
 * reads characters and reports faithfully what the file said. Whether {@code SPACE} is a registered
 * scope, whether {@code form:read} is a declared permission and whether a condition compiles are all
 * questions for {@code PolicyBinder}, which has the catalogues.
 *
 * <p>That separation is the point. A parser bug and a policy bug then look nothing alike — one is a
 * syntax error with a {@link SourceSpan}, the other is "this installation has no scope called SPCE",
 * and neither can masquerade as the other.
 *
 * @param name from the {@code policy "…"} header, or the file name. Carried into every grant's
 *             provenance, so the control room can say <em>granted by policy example</em> rather than
 *             showing an origin of nothing
 */
public record PolicyDocument(
        String                            name,
        List<PolicyInclude>               includes,
        List<PolicyScopeDeclaration>      scopes,
        List<PolicyPermissionDeclaration> permissions,
        List<PolicyActionDeclaration>     actions,
        List<PolicyVariableDeclaration>   variables,
        List<PolicyCapabilityDeclaration> capabilities,
        List<PolicyRole>                  roles,
        List<PolicyPlan>                  plans,
        List<PolicySubject>               subjects,
        List<PolicyEntitlement>           entitlements
) {

    public PolicyDocument {
        includes     = includes     == null ? List.of() : List.copyOf(includes);
        scopes       = scopes       == null ? List.of() : List.copyOf(scopes);
        permissions  = permissions  == null ? List.of() : List.copyOf(permissions);
        actions      = actions      == null ? List.of() : List.copyOf(actions);
        variables    = variables    == null ? List.of() : List.copyOf(variables);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        roles        = roles        == null ? List.of() : List.copyOf(roles);
        plans        = plans        == null ? List.of() : List.copyOf(plans);
        subjects     = subjects     == null ? List.of() : List.copyOf(subjects);
        entitlements = entitlements == null ? List.of() : List.copyOf(entitlements);
    }

    /** An empty document, for a file that declared nothing. */
    public static PolicyDocument empty(String name) {
        return new PolicyDocument(name, List.of(), List.of(), List.of(), List.of(), List.of(),
                                  List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** A document that grants but states no vocabulary — the common case, and every file today. */
    public static PolicyDocument of(String name, List<PolicyRole> roles, List<PolicySubject> subjects) {
        return new PolicyDocument(name, List.of(), List.of(), List.of(), List.of(), List.of(),
                                  List.of(), roles, List.of(), subjects, List.of());
    }

    /** The same, with the tier catalogue as well — what a projection of every row comes to. */
    public static PolicyDocument of(String name, List<PolicyRole> roles, List<PolicySubject> subjects,
                                    List<PolicyPlan> plans) {

        return new PolicyDocument(name, List.of(), List.of(), List.of(), List.of(), List.of(),
                                  List.of(), roles, plans, subjects, List.of());
    }

    /**
     * A document that is nothing but a tier catalogue.
     *
     * <p>⚠️ For a product whose tiers are <strong>rows</strong> and which still wants
     * {@link org.jmouse.access.policy.PolicyPlans#contentsOf} to resolve {@code extends} — lineage
     * walking and cycle detection written once rather than once per storage choice. The document is
     * derived, used for the length of one call and thrown away; nothing edits it and nothing stores it.
     */
    public static PolicyDocument ofPlans(String name, List<PolicyPlan> plans) {
        return new PolicyDocument(name, List.of(), List.of(), List.of(), List.of(), List.of(),
                                  List.of(), List.of(), plans, List.of(), List.of());
    }

    /**
     * Whether this file states the vocabulary as well as the grants.
     *
     * <p>What it means to state it depends on who is listening — see
     * {@code PolicyVocabulary}: an installation that already registered its scopes in code gets the
     * file <em>checked against</em> them, and one that registered nothing gets them <em>built from</em>
     * it.
     */
    public boolean declaresVocabulary() {
        return !scopes.isEmpty() || !permissions.isEmpty() || !actions.isEmpty()
               || !variables.isEmpty() || !capabilities.isEmpty();
    }

    public Optional<PolicyActionDeclaration> action(String actionName) {
        return actions.stream().filter(action -> action.name().equals(actionName)).findFirst();
    }

    public Optional<PolicyVariableDeclaration> variable(String variableName) {
        return variables.stream().filter(variable -> variable.name().equals(variableName)).findFirst();
    }

    /**
     * Whether this file says anything about the entitlement axis.
     *
     * <p>⚠️ What decides whether a store is <strong>required</strong>. A document declaring
     * capabilities or plans where nothing can resolve them is the same class of hole as a condition
     * nothing evaluates: a rule that is written down and does nothing is worse than no rule, because
     * somebody has read it and believes it.
     */
    public boolean declaresEntitlements() {
        return !capabilities.isEmpty() || !plans.isEmpty() || !entitlements.isEmpty();
    }

    public Optional<PolicyRole> role(String roleName) {
        return roles.stream().filter(role -> role.name().equals(roleName)).findFirst();
    }

    public Optional<PolicyPlan> plan(String planCode) {
        return plans.stream().filter(plan -> plan.code().equals(planCode)).findFirst();
    }

    public Optional<PolicySubject> subject(String subjectId) {
        return subjects.stream().filter(subject -> subject.id().equals(subjectId)).findFirst();
    }

    /** Whether anything here is conditional — what decides if a compiler is required. */
    public boolean hasConditions() {
        return subjects.stream()
                .flatMap(subject -> subject.grants().stream())
                .anyMatch(PolicyGrant::isConditional);
    }
}
