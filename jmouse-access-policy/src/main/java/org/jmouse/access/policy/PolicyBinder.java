package org.jmouse.access.policy;

import org.jmouse.access.CapabilityCatalog;
import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeNature;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.policy.AccessPolicy.BoundAssignment;
import org.jmouse.access.policy.AccessPolicy.BoundSubject;
import org.jmouse.access.policy.model.PolicyBundleEntry;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyGrant;
import org.jmouse.access.policy.model.PolicyRole;
import org.jmouse.access.policy.model.PolicyRoleAssignment;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.access.policy.model.PolicySubject;
import org.jmouse.access.policy.model.SourceSpan;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantCondition;
import org.jmouse.access.spi.GrantOrigin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

/**
 * Turns what a file said into what it means — stage two, and the only place that knows both the
 * document model and the engine's vocabulary.
 *
 * <p>Everything questionable is questioned here: an unregistered scope, a permission nobody declared,
 * a place named without an instance, a role assigned that no file declares. A parser reports syntax;
 * this reports <em>model</em> problems, and the two never masquerade as each other.
 *
 * <p><strong>Every problem, not the first one.</strong> A configuration file with six mistakes should
 * report six, not make somebody fix one and run again five times. They are collected and thrown
 * together with their line numbers.
 */
public final class PolicyBinder {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final ScopeCatalog        scopes;
    private final PermissionCatalog   permissions;
    private final ConditionCompiler   conditions;
    private final PlaceholderResolver placeholders;

    /**
     * ⚠️ Empty unless a product registers one, and an empty catalogue checks nothing.
     *
     * <p>Not a constructor parameter, deliberately: every product that has not adopted the entitlement
     * axis keeps compiling and keeps behaving exactly as before. A capability block in a file where
     * nothing registers capabilities is caught by the wiring instead — see issue 11 — because *that*
     * is where the difference between "no axis" and "an axis with nowhere to resolve" is known.
     */
    private CapabilityCatalog capabilities = CapabilityCatalog.empty();

    public PolicyBinder(ScopeCatalog scopes, PermissionCatalog permissions) {
        this(scopes, permissions, null, PlaceholderResolver.none());
    }

    /**
     * @param conditions   how to compile an {@code if} clause, or null where this installation has no
     *                     expression language — in which case a conditional grant refuses to bind
     * @param placeholders how to fill a {@code ${…}}
     */
    public PolicyBinder(
            ScopeCatalog        scopes,
            PermissionCatalog   permissions,
            ConditionCompiler   conditions,
            PlaceholderResolver placeholders) {

        this.scopes       = scopes;
        this.permissions  = permissions;
        this.conditions   = conditions;
        this.placeholders = placeholders == null ? PlaceholderResolver.none() : placeholders;
    }

    /**
     * @param document a document with its {@code include}s already merged in — resolving those is a
     *                 loader's job, because "relative to what" and cycle detection are both questions
     *                 about a whole load rather than about one file
     */
    /**
     * A binder whose vocabulary is the document's own — for a product that keeps its floors in
     * configuration rather than in an enum.
     */
    public static PolicyBinder declaredBy(PolicyDocument document) {
        return new PolicyBinder(
                PolicyVocabulary.scopesOf(document), PolicyVocabulary.permissionsOf(document));
    }

    /**
     * The same binder, also checking the entitlement axis's vocabulary.
     *
     * @param registered what this installation registers as grantable
     */
    public PolicyBinder checking(CapabilityCatalog registered) {
        this.capabilities = registered == null ? CapabilityCatalog.empty() : registered;
        return this;
    }

    public AccessPolicy bind(PolicyDocument document) {
        // A file may also state the vocabulary it is written against. Where it does, it is checked
        // rather than believed: this installation's scopes come from code, and a file that quietly
        // described different ones would be documentation that had stopped being true.
        PolicyVocabulary.checkAgainst(document, scopes, permissions, capabilities);

        List<PolicyProblem> problems = new ArrayList<>();

        Map<String, List<BundledPermission>> roles = bindRoles(document, problems);
        Map<String, BoundSubject> subjects = bindSubjects(document, roles.keySet(), problems);

        if (!problems.isEmpty()) {
            throw new PolicyException(
                    "Policy '" + document.name() + "' does not describe this installation:\n  - "
                    + problems.stream().map(PolicyProblem::toString).collect(joining("\n  - ")),
                    problems);
        }

        return new AccessPolicy(document.name(), roles, subjects, LocalDateTime.now());
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    private Map<String, List<BundledPermission>> bindRoles(
            PolicyDocument document, List<PolicyProblem> problems) {

        Map<String, List<BundledPermission>> bundles = new LinkedHashMap<>();

        for (PolicyRole role : document.roles()) {
            if (bundles.containsKey(role.name())) {
                problems.add(PolicyProblem.at(role.at(), "role '" + role.name() + "' is declared twice. "
                             + "Which bundle wins would be a question about file order, and file order "
                             + "must not decide what anybody may do."));
                continue;
            }

            bundles.put(role.name(), bindBundle(role, problems));
        }

        return bundles;
    }

    private List<BundledPermission> bindBundle(PolicyRole role, List<PolicyProblem> problems) {
        List<BundledPermission> bundle = new ArrayList<>();

        for (PolicyBundleEntry entry : role.bundle()) {
            ScopeKind carriedAt = scopes.byName(entry.scope()).orElse(null);

            if (carriedAt == null) {
                problems.add(PolicyProblem.at(entry.at(), unknownScope(entry.scope())));
                continue;
            }

            for (String permission : expand(entry.permission(), entry.at(), problems)) {
                bundle.add(new BundledPermission(permission, carriedAt));
            }
        }

        return bundle;
    }

    // ── Subjects ──────────────────────────────────────────────────────────────

    private Map<String, BoundSubject> bindSubjects(
            PolicyDocument document, Set<String> declaredRoles, List<PolicyProblem> problems) {

        Map<String, BoundSubject> bound = new LinkedHashMap<>();

        for (PolicySubject subject : document.subjects()) {
            String identifier = fill(subject.id());

            if (bound.containsKey(identifier)) {
                problems.add(PolicyProblem.at(
                        subject.at(), "subject '" + identifier + "' is declared twice."));
                continue;
            }

            bound.put(identifier, new BoundSubject(
                    bindAssignments(subject, declaredRoles, problems),
                    bindGrants(subject, document.name(), problems)));
        }

        return bound;
    }

    private List<BoundAssignment> bindAssignments(
            PolicySubject subject, Set<String> declaredRoles, List<PolicyProblem> problems) {

        List<BoundAssignment> assignments = new ArrayList<>();

        for (PolicyRoleAssignment assignment : subject.roles()) {
            if (!declaredRoles.contains(assignment.roleName())) {
                problems.add(PolicyProblem.at(assignment.at(), "no role called '" + assignment.roleName()
                             + "' is declared. Known roles: " + declaredRoles + "."));
                continue;
            }

            placed(assignment.scope(), assignment.at(), problems)
                    .ifPresent(where -> assignments.add(new BoundAssignment(
                            assignment.roleName(), where, writtenAt(assignment.at()))));
        }

        return assignments;
    }

    private List<DirectGrant> bindGrants(
            PolicySubject subject, String policyName, List<PolicyProblem> problems) {

        List<DirectGrant> grants = new ArrayList<>();

        for (PolicyGrant grant : subject.grants()) {
            ScopeReference where = placed(grant.scope(), grant.at(), problems).orElse(null);

            if (where == null) {
                continue;
            }

            GrantCondition condition = compiled(grant, problems);

            if (grant.isConditional() && condition == null) {
                continue;
            }

            boolean allowed = grant.effect() == PolicyEffect.ALLOW;

            for (String permission : expand(grant.permission(), grant.at(), problems)) {
                grants.add(new DirectGrant(
                        permission, allowed, where,
                        "policy:" + policyName,
                        "declared at " + grant.at(),
                        LocalDateTime.now(),
                        writtenAt(grant.at()),
                        condition));
            }
        }

        return grants;
    }

    /**
     * A grant's condition, compiled — or nothing, with the reason recorded.
     *
     * <p>⚠️ <strong>A conditional grant with no compiler refuses to bind, and always will.</strong>
     * Not "ignore the condition and grant anyway", which is a hole with a comment explaining itself;
     * and not "drop the grant" either, because a conditional <em>deny</em> silently dropped grants
     * more than the file said — the same failure wearing the opposite sign.
     */
    private GrantCondition compiled(PolicyGrant grant, List<PolicyProblem> problems) {
        if (!grant.isConditional()) {
            return null;
        }

        if (conditions == null) {
            problems.add(PolicyProblem.at(grant.at(), conditionsUnsupported(grant.condition())));
            return null;
        }

        try {
            return conditions.compile(grant.condition());
        } catch (RuntimeException broken) {
            problems.add(PolicyProblem.at(grant.at(), "the condition `" + grant.condition()
                         + "` will not compile: " + broken.getMessage()));
            return null;
        }
    }

    /**
     * Where a declaration is written, as the engine's own way of saying it.
     *
     * <p>Only meaningful once a loader has said which file — a document parsed straight from text has
     * nothing to attribute, and claiming a file it does not have would be worse than saying nothing.
     */
    private static GrantOrigin writtenAt(SourceSpan span) {
        return span.namesADocument()
                ? GrantOrigin.declaredIn(span.document(), span.line())
                : GrantOrigin.stored();
    }

    // ── Shared checks ─────────────────────────────────────────────────────────

    /**
     * A scope written by a subject, as a reference — checking that a place names which one.
     *
     * <p>⚠️ This is the escalation guard. A place written without an instance would, taken literally,
     * be a grant in <em>every</em> instance of that place at once. The parser rejects the shape inside
     * a role; here is where it is rejected for a subject, where the catalogue is what says which kinds
     * are places.
     */
    private Optional<ScopeReference> placed(
            PolicyScope scope, SourceSpan where, List<PolicyProblem> problems) {

        ScopeKind kind = scopes.byName(scope.kind()).orElse(null);

        if (kind == null) {
            problems.add(PolicyProblem.at(where, unknownScope(scope.kind())));
            return Optional.empty();
        }

        if (kind.nature() == ScopeNature.PLACE && !scope.namesAnInstance()) {
            problems.add(PolicyProblem.at(where, "'@" + scope.kind() + "' names a kind of place but not "
                         + "which one. Written here that would grant in every " + scope.kind()
                         + " at once — write '@" + scope.kind() + ":<id>'."));
            return Optional.empty();
        }

        if (kind.nature() != ScopeNature.PLACE && scope.namesAnInstance()) {
            problems.add(PolicyProblem.at(where, "'@" + scope.kind() + "' names no instance, so ':"
                         + scope.instance() + "' means nothing here."));
            return Optional.empty();
        }

        return Optional.of(kind.nature() == ScopeNature.PLACE
                ? ScopeReference.of(kind, fill(scope.instance()))
                : ScopeReference.of(kind, ScopeKind.NO_INSTANCE));
    }

    /**
     * One permission, or every permission a namespace wildcard covers.
     *
     * <p>Expanded here rather than matched per request, so the grant set stays concrete and the
     * control room can list what somebody actually holds — which is the question it exists to answer.
     */
    private List<String> expand(String permission, SourceSpan where, List<PolicyProblem> problems) {
        if (permission.endsWith(":*")) {
            String       namespace = permission.substring(0, permission.length() - 2);
            Set<String>  matched   = permissions.inNamespace(namespace);

            if (matched.isEmpty()) {
                problems.add(PolicyProblem.at(
                        where, "'" + permission + "' matches no declared permission."));
            }

            return List.copyOf(matched);
        }

        if (!permissions.contains(permission)) {
            problems.add(PolicyProblem.at(
                    where, "no permission called '" + permission + "' is declared."));
            return List.of();
        }

        return List.of(permission);
    }

    private String fill(String text) {
        if (text == null || !text.contains("${")) {
            return text;
        }

        Matcher      matcher = PLACEHOLDER.matcher(text);
        StringBuilder filled = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(filled, Matcher.quoteReplacement(placeholders.resolve(matcher.group(1))));
        }
        matcher.appendTail(filled);

        return filled.toString();
    }

    private String unknownScope(String name) {
        return "no scope called '" + name + "' is registered. Known scopes: "
               + scopes.all().stream().map(ScopeKind::name).toList() + ".";
    }

    /**
     * ⚠️ A conditional grant refuses to bind where nothing can read its condition.
     *
     * <p>Not "ignore the condition and grant anyway" — that is a hole with a comment explaining
     * itself. Not "drop the grant" either: a conditional <em>deny</em> silently dropped grants more
     * than the file said, which is the same failure wearing the opposite sign.
     */
    private String conditionsUnsupported(String source) {
        return "this grant is conditional (`when " + source + "`) and no ConditionCompiler is "
               + "registered, so nothing can read it. Binding it anyway would either grant "
               + "unconditionally or drop a denial, and both are worse than refusing to start — "
               + "register a compiler, and declare the axis that evaluates one.";
    }
}
