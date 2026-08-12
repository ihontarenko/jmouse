package org.jmouse.access.policy;

import org.jmouse.access.ActionCatalog;
import org.jmouse.access.CapabilityCatalog;
import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.PlaceholderResolver;
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
import org.jmouse.access.spi.GrantAttribution;
import org.jmouse.access.spi.GrantCondition;
import org.jmouse.access.spi.GrantOrigin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.TreeSet;

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

    /**
     * The subject id that means every account — {@code subject * { … }}.
     *
     * <p>⚠️ It cannot collide with a real one: no account identifier is a single asterisk, and the
     * grammar reads {@code *} in a subject's position as this and nothing else.
     */
    public static final String EVERYBODY = "*";

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

    /**
     * ⚠️ Empty unless a product publishes actions, and an empty catalogue checks nothing.
     *
     * <p>Not a constructor parameter, for the reason the capability catalogue is not: a product that
     * has never written {@code @AccessContext} keeps compiling and keeps behaving exactly as before.
     */
    private ActionCatalog actions = ActionCatalog.empty();

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

    /**
     * The same binder, also checking what this installation's calls say they are doing.
     *
     * @param published every action the routes publish, and the values each carries
     */
    public PolicyBinder checking(ActionCatalog published) {
        this.actions = published == null ? ActionCatalog.empty() : published;
        return this;
    }

    public AccessPolicy bind(PolicyDocument document) {
        // A file may also state the vocabulary it is written against. Where it does, it is checked
        // rather than believed: this installation's scopes come from code, and a file that quietly
        // described different ones would be documentation that had stopped being true.
        PolicyVocabulary.checkAgainst(document, scopes, permissions, capabilities, actions);

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

            GrantCondition condition = compiled(entry.condition(), entry.at(), problems);

            // ⚠️ A conditional entry whose condition will not compile is dropped rather than bound
            // unconditionally. A bundle carries no `deny`, so this `when` can only subtract from an
            // allow — and binding it without its condition would hand the permission to everybody
            // holding the role, which is the opposite of what the line says.
            if (entry.isConditional() && condition == null) {
                continue;
            }

            for (String permission : expand(entry.permission(), entry.at(), problems)) {
                bundle.add(new BundledPermission(permission, carriedAt, condition));
            }
        }

        return bundle;
    }

    // ── Subjects ──────────────────────────────────────────────────────────────

    private Map<String, BoundSubject> bindSubjects(
            PolicyDocument document, Set<String> declaredRoles, List<PolicyProblem> problems) {

        Map<String, BoundSubject> bound = new LinkedHashMap<>();

        for (PolicySubject subject : document.subjects()) {
            // ⚠️ The wildcard is not filled. A placeholder resolving to '*' would be a rule about
            // everybody arriving from a property nobody reading the file can see.
            String identifier = EVERYBODY.equals(subject.id()) ? EVERYBODY : fill(subject.id());

            if (bound.containsKey(identifier)) {
                problems.add(PolicyProblem.at(
                        subject.at(), "subject '" + identifier + "' is declared twice."));
                continue;
            }

            if (EVERYBODY.equals(identifier)) {
                refuseAnythingButADenial(subject, problems);
            }

            bound.put(identifier, new BoundSubject(
                    bindAssignments(subject, document.name(), declaredRoles, problems),
                    bindGrants(subject, document.name(), problems)));
        }

        return bound;
    }

    /**
     * ⚠️ <strong>{@code subject *} may only take away.</strong>
     *
     * <p>A block about every account at once is the one place in this language where a mistake reaches
     * everybody, so it is restricted to the one shape that cannot open a door: a denial.
     *
     * <ul>
     *   <li><strong>An allow</strong> written here grants something to every account in the
     *       installation, including ones that do not exist yet, and it does it from a file no screen
     *       can revoke. That is not a rule, it is a hole with a comment explaining itself — and if it
     *       genuinely is meant, it is a role, where it can be seen, audited and taken away.
     *   <li><strong>A role assignment</strong> is the same hole wearing a bundle, and a worse one: what
     *       it grants changes whenever somebody edits the role.
     * </ul>
     *
     * <p>What is left is exactly what the wildcard is for — <em>nobody may do this here</em>, written
     * once instead of once per account. It fits the model's own sentence: deny wins, and the
     * subtraction runs last.
     */
    private void refuseAnythingButADenial(PolicySubject subject, List<PolicyProblem> problems) {
        for (PolicyRoleAssignment assignment : subject.roles()) {
            problems.add(PolicyProblem.at(assignment.at(), "'subject *' assigns the role '"
                         + assignment.roleName() + "' to every account in the installation, from a file "
                         + "nothing at runtime can revoke. A role everybody should hold is a role "
                         + "everybody is given, where it can be seen and taken away."));
        }

        for (PolicyGrant grant : subject.grants()) {
            if (grant.effect() != PolicyEffect.DENY) {
                problems.add(PolicyProblem.at(grant.at(), "'subject *' allows '" + grant.permission()
                             + "' to every account in the installation. A wildcard subject may only "
                             + "deny: it is how a rule says 'nobody may do this here' once instead of "
                             + "once per account, and an allow written the same way is a hole nobody "
                             + "would notice reading."));
            }
        }
    }

    private List<BoundAssignment> bindAssignments(
            PolicySubject       subject,
            String              policyName,
            Set<String>         declaredRoles,
            List<PolicyProblem> problems) {

        List<BoundAssignment> assignments = new ArrayList<>();

        for (PolicyRoleAssignment assignment : subject.roles()) {
            if (!declaredRoles.contains(assignment.roleName())) {
                problems.add(PolicyProblem.at(assignment.at(), "no role called '" + assignment.roleName()
                             + "' is declared. Known roles: " + declaredRoles + "."));
                continue;
            }

            GrantCondition condition = compiled(assignment.condition(), assignment.at(), problems);

            // ⚠️ Same rule as a conditional grant: a condition that will not compile drops the whole
            // assignment. Handing the role out unconditionally would grant more than the file said.
            if (assignment.isConditional() && condition == null) {
                continue;
            }

            GrantAttribution attribution =
                    declared(policyName, assignment.at()).narrowedBy(condition);

            placed(assignment.scope(), assignment.at(), problems).ifPresent(where ->
                    assignments.add(new BoundAssignment(assignment.roleName(), where, attribution)));
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

            GrantCondition condition = compiled(grant.condition(), grant.at(), problems);

            if (grant.isConditional() && condition == null) {
                continue;
            }

            boolean          allowed     = grant.effect() == PolicyEffect.ALLOW;
            GrantAttribution attribution = declared(policyName, grant.at()).narrowedBy(condition);

            for (String permission : expand(grant.permission(), grant.at(), problems)) {
                grants.add(new DirectGrant(permission, allowed, where, attribution));
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
    private GrantCondition compiled(String source, SourceSpan at, List<PolicyProblem> problems) {
        if (source == null || source.isBlank()) {
            return null;
        }

        if (conditions == null) {
            problems.add(PolicyProblem.at(at, conditionsUnsupported(source)));
            return null;
        }

        GrantCondition compiled;

        try {
            compiled = conditions.compile(source);
        } catch (RuntimeException broken) {
            problems.add(PolicyProblem.at(
                    at, "the condition `" + source + "` will not compile: " + broken.getMessage()));
            return null;
        }

        // ⚠️ Outside the catch above, deliberately. Reading a condition for names and compiling one
        // are different questions, and a compiler whose `mentions` throws would otherwise be reported
        // as a condition that will not compile — sending somebody to fix a rule that is fine.
        checkTheRuleCanEverFire(source, at, problems);

        return compiled;
    }

    /**
     * How a rule written in this document names itself.
     *
     * <p>One place, so that a grant, an assignment and a bundle entry cannot come to describe their
     * own provenance three slightly different ways.
     */
    private GrantAttribution declared(String policyName, SourceSpan at) {
        GrantOrigin origin = writtenAt(at);

        return new GrantAttribution(
                "policy:" + policyName, "declared at " + at, LocalDateTime.now(), origin, null);
    }

    /**
     * ⚠️ The pair check — a rule naming an action and a value that action never carries.
     *
     * <pre>
     * deny when action == 'label.print' and purpose != 'HOLDER'
     * </pre>
     *
     * <p>Both names exist. {@code label.print} is a real action; {@code purpose} really is published —
     * by a different route. So this rule never fires, and the administrator who wrote it believes
     * printing is closed. Checking the names one at a time passes it; only checking them
     * <strong>together</strong> catches it, and that is possible only because an action is its own
     * member and there is therefore something to check the value against.
     *
     * <p>Two rules are deliberately let through:
     *
     * <ul>
     *   <li><strong>A rule scoped to no action</strong> can only be name-checked, because a value is
     *       legitimate wherever anything publishes it. That is the right incentive rather than a gap:
     *       an unscoped rule holds everywhere in a call, which is almost never what anybody means.
     *   <li><strong>A rule the compiler could not read exactly.</strong> Guessing would refuse rules
     *       that work, and a validator that refuses correct rules is one somebody switches off.
     * </ul>
     */
    private void checkTheRuleCanEverFire(String source, SourceSpan at, List<PolicyProblem> problems) {
        ConditionMentions mentions = conditions.mentions(source);

        // ⚠️ Runs whatever the action catalogue holds. A path is wrong or right on its own — it is
        // checked against a type this engine owns, not against what some route happens to publish.
        checkEveryPathResolves(mentions, at, problems);

        if (actions.publishedValuesByAction().isEmpty() || !mentions.isCheckable()) {
            return;
        }

        for (String action : mentions.actions()) {
            if (!actions.contains(action)) {
                problems.add(PolicyProblem.at(at, "this rule is scoped to the action '" + action
                             + "', which no route publishes, so it can never fire. Known actions: "
                             + actions.all() + "."));
            }
        }

        if (!mentions.namesAnAction()) {
            checkEveryValueIsPublishedBySomething(source, at, mentions, problems);
            return;
        }

        for (String value : mentions.values()) {
            if (isPublishedByAny(mentions.actions(), value)) {
                continue;
            }

            problems.add(PolicyProblem.at(at, "this rule reads '" + value + "' and is scoped to "
                         + describe(mentions.actions()) + ", which publishes " + publishedBy(mentions.actions())
                         + ". Both names exist, so nothing else would object — and the rule would never "
                         + "fire, which reads exactly like a rule that works."));
        }
    }

    /**
     * ⚠️ <strong>A property nobody declares is a rule that loads clean and never holds.</strong>
     *
     * <p>The failure this check exists for, in one line:
     *
     * <pre>{@code @GLOBAL user:write when caller.name is contains ('GOD')}</pre>
     *
     * <p>Every token is legal, so the vocabulary passes it. It compiles. It names no action, so the
     * pair check above lets it through. It boots. And it can never hold, because whatever
     * {@code caller} is bound to has no such member — which costs one person a permission on a
     * personal grant, and is a <em>silent mass denial</em> inside a role, since a conditional allow
     * that cannot hold subtracts from everybody who holds it.
     *
     * <p>It is the same shape as the quoted index {@code ConditionVocabulary} already refuses: a rule
     * that survives load and answers wrongly on every request is the worst way to be wrong, so it is
     * caught here, where the message can name the members that do exist.
     *
     * <p>⚠️ <strong>{@code resource} is deliberately not checked and must never be.</strong> It is a
     * product's own row, polymorphic across every route that resolves one, and a checker that guessed
     * would refuse rules that work — which is how a validator gets switched off.
     */
    private void checkEveryPathResolves(
            ConditionMentions mentions, SourceSpan at, List<PolicyProblem> problems) {

        mentions.paths().forEach((head, members) -> {
            List<String> declared = ConditionHeads.membersOf(head);

            if (declared == null) {
                return;
            }

            members.stream().filter(member -> !declared.contains(member)).forEach(member ->
                    problems.add(PolicyProblem.at(at, "this rule reads '" + head + "." + member
                                 + "', and " + head + " has no '" + member + "'. It would load and never "
                                 + "hold, which reads exactly like a rule that works. Known members: "
                                 + declared + ".")));
        });
    }

    /**
     * The weaker half, for a rule that bounds itself to nothing: every name it reads must at least be
     * published by <em>something</em>, or it is a typo nobody would ever see.
     */
    private void checkEveryValueIsPublishedBySomething(
            String source, SourceSpan at, ConditionMentions mentions, List<PolicyProblem> problems) {

        Set<String> anywhere = actions.everyPublishedValue();

        for (String value : mentions.values()) {
            if (!anywhere.contains(value)) {
                problems.add(PolicyProblem.at(at, "this rule reads '" + value + "', which no "
                             + "route publishes anywhere. Published values: " + anywhere + "."));
            }
        }
    }

    private boolean isPublishedByAny(Set<String> named, String value) {
        return named.stream().anyMatch(action -> actions.valuesOf(action).contains(value));
    }

    private String describe(Set<String> named) {
        return named.size() == 1
                ? "'" + named.iterator().next() + "'"
                : named.stream().map(action -> "'" + action + "'").collect(joining(" or "));
    }

    private String publishedBy(Set<String> named) {
        Set<String> published = new TreeSet<>();

        named.forEach(action -> published.addAll(actions.valuesOf(action)));

        return published.isEmpty() ? "nothing" : published.toString();
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
        return PlaceholderResolver.fill(text, placeholders);
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
