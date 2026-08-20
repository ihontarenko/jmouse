package org.jmouse.access.projection;

import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.el.PolicyWriter;
import org.jmouse.access.jpa.AccessAdministration;
import org.jmouse.access.jpa.AccessAdministration.BundleEntry;
import org.jmouse.access.jpa.AccessAdministration.RoleView;
import org.jmouse.access.jpa.AccessDisclosure;
import org.jmouse.access.jpa.AccessDisclosure.DirectHolding;
import org.jmouse.access.jpa.AccessDisclosure.RoleHolding;
import org.jmouse.access.policy.PolicyVocabulary;
import org.jmouse.access.policy.model.PolicyBundleEntry;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.access.policy.model.PolicyGrant;
import org.jmouse.access.policy.model.PolicyPermissionDeclaration;
import org.jmouse.access.policy.model.PolicyPlan;
import org.jmouse.access.policy.model.PolicyRole;
import org.jmouse.access.policy.model.PolicyRoleAssignment;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.access.ScopeNature;
import org.jmouse.access.policy.model.PolicyScopeDeclaration;
import org.jmouse.access.policy.model.PolicySubject;
import org.jmouse.access.policy.model.SourceSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * <strong>The authorization as the tables hold it right now, as the document that describes it.</strong>
 *
 * <h2>⚠️ The file on disk is the SEED, and the rows are the truth</h2>
 *
 * <p>A {@code policy/*.jmp} document is what a fresh installation is born with. Where the composition
 * refuses to union that document back over the tables — the ordinary arrangement — the engine reads
 * rows from the first boot onwards, and the file drifts from them the moment somebody edits a bundle
 * on an access screen. There is otherwise nowhere to read what is <em>actually in force</em> in one
 * piece: a screen shows roles, holders and personal overrides as three lists, and a person answering
 * <em>"who can do what here"</em> assembles it in their head.
 *
 * <p>⚠️ <strong>So this projects from ROWS and never from the shipped document.</strong> Rendering the
 * file would make this a second, prettier copy of exactly the thing that is out of date.
 *
 * <h2>⚠️ It builds a document. It does not write one</h2>
 *
 * <p>This is the half that knows the tables; {@link PolicyWriter} is the half that knows the grammar,
 * and it is the <em>only</em> half that does — it builds the very nodes the parser builds and asks
 * them to render, so it cannot drift from the parser because it is not a separate implementation of
 * anything.
 *
 * <p>⚠️ There was a second implementation here once: a projection that appended strings by hand and
 * so spelled the grammar a second time. It got the grammar backwards — {@code allow issue:assign
 * @PROJECT 'TSSR'} where the language reads {@code @PROJECT:'TSSR' issue:assign} — and it wrote every
 * identifier bare, so an installation whose subjects are UUIDs rendered a document the lexer read as
 * a chain of subtractions. Both failures are the same failure, and neither is reachable from here:
 * everything below produces model records, and quoting is {@code SourceWriter}'s single decision.
 *
 * <h2>⚠️ Read-only, and it stays that way</h2>
 *
 * <p>There is no parser here and no "apply this document". A screen writes through
 * {@link AccessAdministration}, which validates every change against the catalogues. Innoventa's
 * editor is the exception that proves it: it parses submitted text with the real parser and applies
 * it through that same port, rather than treating a rendering as a source.
 *
 * <h2>⚠️ Whatever reads this discloses the whole installation</h2>
 *
 * <p>It is assembled from {@link AccessDisclosure}, so every caveat on that interface applies whole: a
 * product exposing this puts it behind a permission of its own, because one call answers <em>who holds
 * what, everywhere</em>.
 *
 * <h2>Ordered, and the order is part of the contract</h2>
 *
 * <p>Roles, subjects and the lines inside them are sorted by name. The document this produces is
 * diffed against the one somebody submits, shown in a code pane and stored as a revision — and a
 * rendering whose line order came out of a query plan would make every diff meaningless and every
 * revision look edited. Permissions and scopes are the two exceptions and keep their catalogue order,
 * because for a scope block that order <em>is</em> the covering chain.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PolicyProjection {

    /**
     * ⚠️ What a projected document is called where the product has no better name, and therefore what
     * every declaration in it reports as its origin. Not {@code control-room}: this is not somebody's
     * edit, it is the tables saying what they hold.
     */
    public static final String IN_FORCE = "in-force";

    /** A permission the catalogue knows but nothing described, written as the grammar requires. */
    private static final String UNDESCRIBED = "";

    private final String policyName;

    private Collection<String>              permissions    = List.of();
    private UnaryOperator<String>           describe       = permission -> null;
    private Collection<? extends ScopeKind> scopes         = List.of();
    private Collection<RoleView>            roles          = List.of();
    private Collection<RoleHolding>         roleHoldings   = List.of();
    private Collection<DirectHolding>       directHoldings = List.of();
    private Collection<PolicyPlan>          plans          = List.of();
    private UnaryOperator<String>           nameInstance   = identifier -> null;

    private PolicyProjection(String policyName) {
        this.policyName = policyName;
    }

    /**
     * A projection of an installation whose policy carries this name.
     *
     * @param policyName the name to write in the {@code policy "…"} header — the same name the shipped
     *                   document carries, so a reader can tell which installation this describes
     */
    public static PolicyProjection of(String policyName) {
        return new PolicyProjection(policyName);
    }

    /**
     * The vocabulary this installation grants against.
     *
     * <p>⚠️ <strong>Give the whole catalogue, not the permissions somebody happens to hold.</strong>
     * This screen answers <em>what is in force here</em> in one piece, and a permission nobody holds is
     * part of that answer — it is the one an administrator is about to grant.
     *
     * @param permissions every permission this build knows, in catalogue order
     * @param describe    a permission's description, or null where there is none. Written as an empty
     *                    string rather than omitted, because the grammar requires one
     */
    public PolicyProjection permissions(Collection<String> permissions, UnaryOperator<String> describe) {
        this.permissions = permissions;
        this.describe    = describe;

        return this;
    }

    /**
     * The scopes to declare.
     *
     * <p>⚠️ <strong>{@code all()}, never {@code floors()}.</strong> A catalogue's floors are the scopes
     * a grant may name an <em>instance</em> of, which in most products is one scope; the widest scope
     * and own-rows are not places and would be left out. The result declares a vocabulary the engine
     * does not have, above roles whose every entry names a scope the block never mentioned — and it
     * would not parse back.
     *
     * @param scopes the registered scopes, widest first — declaration order IS width order
     */
    public PolicyProjection scopes(Collection<? extends ScopeKind> scopes) {
        this.scopes = scopes;

        return this;
    }

    /**
     * The roles as the tables hold them, bundles included.
     *
     * <p>⚠️ <strong>From {@link AccessAdministration#roles()}, not from the library's projector.</strong>
     * That one answers <em>what do these subjects hold</em>, so a role nobody holds does not appear at
     * all and {@code assignableAt} is not projected — and a screen that lost every unassigned role, or
     * that rendered a role without the clause stopping a workspace role being handed out
     * installation-wide, would be worse than no screen.
     */
    public PolicyProjection roles(Collection<RoleView> roles) {
        this.roles = roles;

        return this;
    }

    /**
     * Who holds what, from the port that exists to answer it installation-wide.
     *
     * <p>⚠️ <strong>Both halves, and a subject appearing in only one still gets a block.</strong> An
     * account with a personal deny and no role is exactly the case somebody opens this screen to look
     * at, and a projection keyed off assignments alone would render nothing for them.
     *
     * <p>⚠️ <strong>A holding whose subject no longer exists is rendered rather than skipped.</strong> A
     * library table cannot foreign-key into a product's accounts, so a grant can outlive the row it was
     * made to — and those orphans are exactly what somebody reading this document is looking for.
     */
    public PolicyProjection holdings(
            Collection<RoleHolding> roleHoldings, Collection<DirectHolding> directHoldings) {

        this.roleHoldings   = roleHoldings;
        this.directHoldings = directHoldings;

        return this;
    }

    /**
     * The tier catalogue, for a product that has one.
     *
     * <p>⚠️ <strong>Rendered exactly as stored, {@code extends} and all.</strong> Flattening a lineage
     * would show an inherited line as if the tier stated it, and anything reading this back would then
     * write it — turning a difference into a copy, after which editing the parent stops reaching the
     * child.
     */
    public PolicyProjection plans(Collection<PolicyPlan> plans) {
        this.plans = plans;

        return this;
    }

    /**
     * What to call a scope instance — <strong>and in a product whose scope contains itself this is not
     * cosmetic.</strong>
     *
     * <p>A line reading {@code grants CATEGORY_READER @CATEGORY:a3f9} is true and radically incomplete:
     * it silently means every section under that one as well. This is where a product turns the
     * identifier into something that says how far the grant reaches — a path, not a name. Where the
     * function answers null the identifier is written as it stands.
     *
     * <p>⚠️ <strong>Naming makes the document read-only in fact as well as in intent.</strong> A name is
     * not an identifier, so nothing can apply the result back as rows. A product whose pane is an
     * editor — Innoventa's — deliberately declares no naming and keeps the identifiers.
     */
    public PolicyProjection naming(UnaryOperator<String> nameScopeInstance) {
        this.nameInstance = nameScopeInstance;

        return this;
    }

    /**
     * Everything declared so far, as one document.
     *
     * @return the projection, ready to be written, diffed or stored as a revision
     */
    public PolicyDocument project() {
        return new PolicyDocument(
                policyName,
                List.of(),
                projectedScopes(),
                projectedPermissions(),
                List.of(),
                List.of(),
                List.of(),
                projectedRoles(),
                List.copyOf(plans),
                projectedSubjects(),
                List.of());
    }

    /**
     * The same document as {@code .jmp} source, under the banner saying where it came from.
     *
     * <p>The banner is comments, and {@link PolicyWriter} neither writes nor keeps those — so it is
     * prepended here rather than modelled. A caller wanting the text alone writes {@link #project()}
     * itself.
     *
     * @return the document, ready to show in a read-only pane
     */
    public String render() {
        return banner(policyName) + PolicyWriter.write(project());
    }

    /**
     * The warning a read-only pane opens with.
     *
     * <p>⚠️ It is not decoration. Everything below it parses, which makes "paste it over the shipped
     * file" an obvious and wrong thing to try — the shipped file is the seed, and a re-seed rewrites
     * whatever it declares.
     *
     * @param policyName the shipped document's name, so the banner can point at the actual file
     */
    public static String banner(String policyName) {
        String rule = "# " + "=".repeat(74) + "\n";

        return rule
               + "#  ⚠️ GENERATED FROM THE TABLES — this is NOT the file on disk.\n"
               + "#\n"
               + "#  `policy/" + policyName + ".jmp` is the SEED: what a fresh\n"
               + "#  installation was born with. Everything below is what the engine actually\n"
               + "#  reads NOW, including every change made on the access screen since.\n"
               + "#\n"
               + "#  ⚠️ Read-only. Pasting this over the shipped file is not how a change is\n"
               + "#  made — the screen writes rows, and a re-seed rewrites whatever the shipped\n"
               + "#  document declares.\n"
               + rule
               + "\n";
    }

    // ── Vocabulary ────────────────────────────────────────────────────────────

    private List<PolicyPermissionDeclaration> projectedPermissions() {
        return permissions.stream()
                .map(permission -> new PolicyPermissionDeclaration(
                        permission, descriptionOf(permission), SourceSpan.none()))
                .toList();
    }

    /** ⚠️ Never null: {@code PermissionDeclarationParser} requires a string, so one is always written. */
    private String descriptionOf(String permission) {
        String description = describe.apply(permission);

        return description == null || description.isBlank() ? UNDESCRIBED : description;
    }

    /**
     * ⚠️ <strong>In the order the catalogue holds them, which is width order.</strong> A projection that
     * sorted these alphabetically would render a document that parses and describes a different covering
     * chain — the one thing about a scope block that is load-bearing is its order.
     */
    private List<PolicyScopeDeclaration> projectedScopes() {
        List<PolicyScopeDeclaration> projected = new java.util.ArrayList<>();
        java.util.Map<String, String> firstUnder = new java.util.LinkedHashMap<>();

        for (ScopeKind scope : scopes) {
            String inside = scope.inside().map(ScopeKind::name).orElse(null);
            String beside = null;

            // ⚠️ Rendered as `beside=` where another place already sits in the same parent, and as
            // `inside=` otherwise. Both say the same thing; the difference is that a reader of a
            // GENERATED file can otherwise not tell a deliberate sibling from a forgotten `inside=` —
            // siblings are the default, and a default is invisible.
            if (scope.nature() == ScopeNature.PLACE) {
                String parent  = inside == null ? "" : inside;
                String sibling = firstUnder.get(parent);

                if (sibling == null) {
                    firstUnder.put(parent, scope.name());
                } else {
                    beside = sibling;
                    inside = null;
                }
            }

            projected.add(new PolicyScopeDeclaration(
                    scope.name(),
                    PolicyVocabulary.spellingOf(scope.nature()),
                    scope.requestParameter().orElse(null),
                    inside,
                    beside,
                    scope.requiredAncestor().map(ScopeKind::name).orElse(null),
                    SourceSpan.none()));
        }

        return List.copyOf(projected);
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    private List<PolicyRole> projectedRoles() {
        return roles.stream()
                .sorted(Comparator.comparing(RoleView::name))
                .map(PolicyProjection::roleOf)
                .toList();
    }

    private static PolicyRole roleOf(RoleView role) {
        List<PolicyBundleEntry> bundle = role.bundle().stream()
                .sorted(Comparator.comparing(BundleEntry::permission)
                        .thenComparing(BundleEntry::scopeType))
                .map(entry -> new PolicyBundleEntry(
                        entry.permission(), entry.scopeType(), entry.conditionSource(), SourceSpan.none()))
                .toList();

        return new PolicyRole(role.name(), role.assignableAt(), bundle, SourceSpan.none());
    }

    // ── Subjects ──────────────────────────────────────────────────────────────

    private List<PolicySubject> projectedSubjects() {
        Map<String, Held> held = new LinkedHashMap<>();

        for (RoleHolding holding : roleHoldings) {
            held.computeIfAbsent(holding.subjectId(), subject -> new Held())
                    .assignments.add(new PolicyRoleAssignment(
                            holding.roleName(), scopeOf(holding.at()), holding.condition(), SourceSpan.none()));
        }

        for (DirectHolding holding : directHoldings) {
            held.computeIfAbsent(holding.subjectId(), subject -> new Held())
                    .grants.add(new PolicyGrant(
                            holding.permission(),
                            scopeOf(holding.at()),
                            holding.allowed() ? PolicyEffect.ALLOW : PolicyEffect.DENY,
                            holding.condition(),
                            SourceSpan.none()));
        }

        return held.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PolicySubject(
                        entry.getKey(),
                        entry.getValue().sortedAssignments(),
                        entry.getValue().sortedGrants(),
                        SourceSpan.none()))
                .toList();
    }

    /**
     * One place, named so a reader can tell how far it reaches.
     *
     * <p>⚠️ A scope that names no instance is written as the kind alone. { ScopeReference} makes
     * that exact rather than a guess: its own constructor refuses an identifier-less place and forces
     * every other kind to the sentinel, so the nature is the whole question.
     */
    private PolicyScope scopeOf(ScopeReference where) {
        if (!where.type().namesAnInstance()) {
            return PolicyScope.kind(where.type().name());
        }

        String named = nameInstance.apply(where.id());

        return PolicyScope.of(where.type().name(), named == null ? where.id() : named);
    }

    /** One account's two lists, while they are being gathered from two ports. */
    private static final class Held {

        private final List<PolicyRoleAssignment> assignments = new ArrayList<>();
        private final List<PolicyGrant>          grants      = new ArrayList<>();

        private List<PolicyRoleAssignment> sortedAssignments() {
            return assignments.stream()
                    .sorted(Comparator.comparing(PolicyRoleAssignment::roleName)
                            .thenComparing(assignment -> assignment.scope().toString()))
                    .toList();
        }

        private List<PolicyGrant> sortedGrants() {
            return grants.stream()
                    .sorted(Comparator.comparing(PolicyGrant::permission)
                            .thenComparing(grant -> grant.scope().toString()))
                    .toList();
        }
    }
}
