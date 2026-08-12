package org.jmouse.access.el;

import org.jmouse.access.policy.model.*;

import java.util.List;
import java.util.Objects;

/**
 * Reads one file using every construction the language has, and checks that all of it arrived.
 *
 * <p>⚠️ <strong>"It parsed" is not the property worth checking.</strong> A parser that quietly keeps
 * four of a subject's five grants parses every file it is given, and the installation it configures
 * is missing a rule nobody will look for. So this asserts the document <em>field by field</em> — the
 * scopes in the order they were written, every bundle entry, every assignment, every grant with its
 * effect and its condition — and reports all the failures it finds rather than the first.</p>
 *
 * <p>The whole check then runs a second time against the file's own {@link
 * ExpressionEvaluator#rewrite(String) rewritten} text. That is the round-trip: what the control room
 * would show an administrator has to say what the file said, and rewriting it again has to change
 * nothing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Smoke {

    /**
     * One file using every construction the language has.
     *
     * <p>Public because it is <strong>the</strong> fixture rather than this class's own: {@link
     * ExchangeSmoke} writes it back out and reads it again over the same text. One fixture read by
     * two surfaces is what makes them drift visibly instead of quietly.
     */
    public static final String DECLARATION = """
            policy "innoventa-bootstrap" {
                include 'startup.jmp'
                scopes {
                    # default scoped that required for start
                    @ORGANIZATION  place  parameter=organizationId # innoventa's organizationId
                    @INSTALLATION  everything
                    @SPACE         place  parameter=spaceId
                    @SELF          'own-rows'
                }
                permissions {
                    form:read     "Read forms"
                    form:write    "Create and edit forms"
                }
                actions {
                    entry.list           "List submissions"
                    entry.listByPurpose  "List one purpose"  publishes purpose, tier
                }
                capabilities {
                    paid parametric-search
                    gate   parametric-search  "Parametric search"
                    limit  workspace          "Workspaces"  per ORGANIZATION
                    quota  storage-byte       "Storage"     per ORGANIZATION, SPACE
                }
                plans {
                    plan free "Free" order 10 note "Room to try the product properly." {
                        workspace     1
                        storage-byte  200MB per month
                    }
                    plan business "Business" order 30 extends free {
                        workspace     25
                        storage-byte  unlimited
                        parametric-search
                    }
                }
                entitlements {
                    @ORGANIZATION:acme  plan business
                    @ORGANIZATION:beta  trial business until 2026-09-12 reason "evaluating"
                    @SPACE:kyiv         allow workspace 5 from 2026-08-01
                    @SPACE:kyiv         deny parametric-search reason "not paid for here"
                }
                role INSTALLATION_OWNER {
                    @INSTALLATION  space:write
                    @INSTALLATION  user:manage
                }
                subject ${innoventa.bootstrap.owner} {
                    @SELF  form:write  deny
                    grants INSTALLATION_OWNER @INSTALLATION
                    grants SELF  @SPACE:kyiv
                    grants SPACE_ADMIN  @SPACE:*
                    grants SPACE_ADMIN  @ORGANIZATION:${application.defaultOrganization}
                    @SCOPE:${application.defaultInstance}  form:write         # a direct grant, allow implied
                    @SCOPE:*  form:*         # a direct grant, allow implied
                    @SCOPE:instance  form:read  allow
                    @SCOPE:instance  form:delete  deny when resource.status == 'DRAFT'
                    # ⚠️ Every name in this condition is a KEYWORD of this grammar, and that is the
                    # whole reason the line exists. The end of a condition used to be found by running
                    # the expression parser over these tokens — so a name like `caller` or `plan`
                    # reached it already lexed as a keyword, and the file failed to PARSE over a word
                    # that is not a keyword at this position at all. Keep a condition made of them.
                    @SCOPE:instance  form:share  deny when caller.plan != null
                    # ⚠️ The braced form, and it exists because the one-line form TRUNCATES. A rule
                    # broken across lines for readability parses as a rule ending after the first —
                    # a weaker rule that still loads and still reads like the one somebody wrote.
                    # The braces are only a delimiter: this must come back as ONE LINE of text.
                    @SCOPE:instance  entry:read  deny when {
                        action == 'entry.listByPurpose'
                        and purpose != 'HOLDER'
                    }
                }
            }
            """;

    private Smoke() {
    }

    public static void main(String[] arguments) {
        ExpressionEvaluator evaluator    = new ExpressionEvaluator();
        Verification        verification = new Verification();

        verifyPolicy(evaluator.parse(DECLARATION), verification.section("as written"));
        verifyRoundTrip(evaluator, verification);

        verification.report();
    }

    /**
     * Checks that everything the file declares is in the document, and nothing else is.
     *
     * @param document     the parsed file
     * @param verification where to record what held and what did not
     */
    public static void verifyPolicy(PolicyDocument document, Verification verification) {
        verification.equal("policy name", "innoventa-bootstrap", document.name());
        verification.equal("declares a vocabulary", true, document.declaresVocabulary());

        verifyIncludes(document.includes(), verification);
        verifyScopes(document.scopes(), verification);
        verifyPermissions(document.permissions(), verification);
        verifyActions(document.actions(), verification);
        verifyCapabilities(document.capabilities(), verification);
        verifyPlans(document.plans(), verification);
        verifyRoles(document.roles(), verification);
        verifySubjects(document.subjects(), verification);
        verifyEntitlements(document.entitlements(), verification);
    }

    /**
     * Checks {@code actions}, and the two things about the block that are easy to get quietly wrong.
     *
     * <p><strong>A dotted name is one word.</strong> The lexer hands over {@code entry}, {@code .} and
     * {@code listByPurpose} separately, exactly as it does a permission's colons, and a reader that
     * kept only the first segment would produce an action every rule fails to match.
     *
     * <p><strong>{@code publishes} is optional, and an empty list is the statement.</strong> An action
     * carrying nothing is still worth declaring — a rule may scope itself to it and compare nothing
     * but its name — so absence has to arrive as "no values" rather than as a null nobody planned for.
     */
    private static void verifyActions(List<PolicyActionDeclaration> actions, Verification verification) {
        if (!verification.size("actions", 2, actions)) {
            return;
        }

        verification.equal("action 1", "entry.list", actions.get(0).name());
        verification.equal("action 1 description", "List submissions", actions.get(0).description());
        verification.equal("action 1 publishes nothing", List.of(), actions.get(0).values());

        verification.equal("action 2", "entry.listByPurpose", actions.get(1).name());
        verification.equal("action 2 publishes", List.of("purpose", "tier"), actions.get(1).values());
    }

    private static void verifyCapabilities(
            List<PolicyCapabilityDeclaration> capabilities, Verification verification) {

        if (!verification.size("capabilities", 3, capabilities)) {
            return;
        }

        verification.equal("capability 1", "parametric-search", capabilities.get(0).key());
        verification.equal("capability 1 name", "Parametric search", capabilities.get(0).displayName());
        verification.equal("capability 1 paid", true, capabilities.get(0).paid());
        // ⚠️ As WRITTEN, not normalised. The engine keeps the word the file used so a message can quote
        // the line back; turning `limit` into a constant is the product's job, at the boundary where it
        // registers its catalogue.
        verification.equal("capability 2 kind", "limit", capabilities.get(1).kind());
        verification.equal("capability 2 scopes", List.of("ORGANIZATION"), capabilities.get(1).scopes());
        verification.equal("capability 3 kind", "quota", capabilities.get(2).kind());

        // ⚠️ A capability the file does not call `paid` is FREE, and the absence is the statement.
        // Writing `free` would make every module in a real catalogue carry a word that says nothing.
        verification.equal("capability 2 paid", false, capabilities.get(1).paid());
    }

    /**
     * Checks {@code plans}, including the two things about it that are easy to get quietly wrong.
     *
     * <p><strong>A derived tier keeps only its own lines.</strong> {@code business extends free} writes
     * three, and resolving the lineage is a reader's job — a parser that flattened it would turn
     * inheritance into a copy that drifts from its base at the first edit.
     *
     * <p><strong>{@code unlimited} is not a quantity.</strong> It arrives as a flag with a null amount,
     * because a ceiling that exists and one that does not are different facts and every screen above
     * has to tell them apart.
     */
    private static void verifyPlans(List<PolicyPlan> plans, Verification verification) {
        if (!verification.size("plans", 2, plans)) {
            return;
        }

        PolicyPlan free     = plans.get(0);
        PolicyPlan business = plans.get(1);

        verification.equal("plan 1 code", "free", free.code());
        verification.equal("plan 1 name", "Free", free.displayName());
        verification.equal("plan 1 order", 10, free.order());
        verification.equal("plan 1 note", "Room to try the product properly.", free.note());
        verification.equal("plan 1 derived", false, free.isDerived());
        verification.equal("plan 1 lines", 2, free.grants().size());
        verification.equal("plan 1 line 2 quantity", "200MB", free.grants().get(1).quantity());
        verification.equal("plan 1 line 2 period", "month", free.grants().get(1).period());

        verification.equal("plan 2 extends", "free", business.extendsCode());
        verification.equal("plan 2 keeps only its own lines", 3, business.grants().size());
        verification.equal("plan 2 unlimited flag", true, business.grants().get(1).unlimited());
        verification.equal("plan 2 unlimited has no amount", null, business.grants().get(1).quantity());
        verification.equal("plan 2 gate is not metered", false, business.grants().get(2).isMetered());
    }

    /**
     * Checks {@code entitlements}, whose whole point is that a window is a <em>qualifier</em> rather
     * than a predicate: {@code from} and {@code until} arrive as fields, so an expired grant stays
     * legible instead of becoming an opaque condition that merely returns false.
     */
    private static void verifyEntitlements(
            List<PolicyEntitlement> entitlements, Verification verification) {

        if (!verification.size("entitlements", 4, entitlements)) {
            return;
        }

        verification.equal("entitlement 1 kind", PolicyEntitlement.Kind.PLAN, entitlements.get(0).kind());
        verification.equal("entitlement 1 at", "ORGANIZATION", entitlements.get(0).at().kind());
        verification.equal("entitlement 1 instance", "acme", entitlements.get(0).at().instance());
        verification.equal("entitlement 1 subject", "business", entitlements.get(0).subject());
        verification.equal("entitlement 1 is a bundle", true, entitlements.get(0).isBundle());

        verification.equal("entitlement 2 kind", PolicyEntitlement.Kind.TRIAL, entitlements.get(1).kind());
        verification.equal("entitlement 2 until", "2026-09-12", entitlements.get(1).until());
        verification.equal("entitlement 2 reason", "evaluating", entitlements.get(1).reason());
        verification.equal("entitlement 2 is bounded", true, entitlements.get(1).isBounded());

        verification.equal("entitlement 3 kind", PolicyEntitlement.Kind.ALLOW, entitlements.get(2).kind());
        verification.equal("entitlement 3 subject", "workspace", entitlements.get(2).subject());
        verification.equal("entitlement 3 quantity", "5", entitlements.get(2).quantity());
        verification.equal("entitlement 3 from", "2026-08-01", entitlements.get(2).from());

        verification.equal("entitlement 4 kind", PolicyEntitlement.Kind.DENY, entitlements.get(3).kind());
        verification.equal("entitlement 4 reason", "not paid for here", entitlements.get(3).reason());
        verification.equal("entitlement 4 is not a bundle", false, entitlements.get(3).isBundle());
    }

    private static void verifyIncludes(List<PolicyInclude> includes, Verification verification) {
        if (!verification.size("includes", 1, includes)) {
            return;
        }

        verification.equal("include path", "startup.jmp", includes.getFirst().path());
        verification.equal("include line", 2, includes.getFirst().at().line());
    }

    /**
     * Checks the {@code scopes} block, <strong>by position</strong> — declaration order is width
     * order, so a file whose floors arrive in the wrong order is a covering chain nobody reordered.
     *
     * @param scopes       the declarations as parsed
     * @param verification where to record what held and what did not
     */
    private static void verifyScopes(List<PolicyScopeDeclaration> scopes, Verification verification) {
        if (!verification.size("scopes", 4, scopes)) {
            return;
        }

        verifyScope(scopes.get(0), "ORGANIZATION", "place", "organizationId", verification);
        verifyScope(scopes.get(1), "INSTALLATION", "everything", null, verification);
        verifyScope(scopes.get(2), "SPACE", "place", "spaceId", verification);
        verifyScope(scopes.get(3), "SELF", "own-rows", null, verification);
    }

    private static void verifyScope(
            PolicyScopeDeclaration declaration,
            String name,
            String nature,
            String parameter,
            Verification verification
    ) {
        verification.equal("scope @" + name, name, declaration.name());
        verification.equal("scope @%s nature".formatted(name), nature, declaration.nature());
        verification.equal("scope @%s parameter".formatted(name), parameter, declaration.parameter());
    }

    private static void verifyPermissions(List<PolicyPermissionDeclaration> permissions, Verification verification) {
        if (!verification.size("permissions", 2, permissions)) {
            return;
        }

        verification.equal("permission 1", "form:read", permissions.get(0).name());
        verification.equal("permission 1 description", "Read forms", permissions.get(0).description());
        verification.equal("permission 2", "form:write", permissions.get(1).name());
        verification.equal("permission 2 description", "Create and edit forms", permissions.get(1).description());
    }

    private static void verifyRoles(List<PolicyRole> roles, Verification verification) {
        if (!verification.size("roles", 1, roles)) {
            return;
        }

        PolicyRole role = roles.getFirst();

        verification.equal("role name", "INSTALLATION_OWNER", role.name());

        if (!verification.size("role bundle", 2, role.bundle())) {
            return;
        }

        verifyBundleEntry(role.bundle().get(0), "space:write", "INSTALLATION", verification);
        verifyBundleEntry(role.bundle().get(1), "user:manage", "INSTALLATION", verification);
    }

    private static void verifyBundleEntry(
            PolicyBundleEntry entry, String permission, String scope, Verification verification
    ) {
        verification.equal("bundle entry " + permission, permission, entry.permission());
        verification.equal("bundle entry %s reaches".formatted(permission), scope, entry.scope());
    }

    private static void verifySubjects(List<PolicySubject> subjects, Verification verification) {
        if (!verification.size("subjects", 1, subjects)) {
            return;
        }

        PolicySubject subject = subjects.getFirst();

        verification.equal("subject id", "${innoventa.bootstrap.owner}", subject.id());

        verifyAssignments(subject.roles(), verification);
        verifyGrants(subject.grants(), verification);
    }

    private static void verifyAssignments(List<PolicyRoleAssignment> assignments, Verification verification) {
        if (!verification.size("role assignments", 4, assignments)) {
            return;
        }

        verifyAssignment(assignments.get(0), "INSTALLATION_OWNER", "INSTALLATION", null, verification);
        verifyAssignment(assignments.get(1), "SELF", "SPACE", "kyiv", verification);
        verifyAssignment(assignments.get(2), "SPACE_ADMIN", "SPACE", "*", verification);
        verifyAssignment(assignments.get(3), "SPACE_ADMIN", "ORGANIZATION",
                "${application.defaultOrganization}", verification);
    }

    private static void verifyAssignment(
            PolicyRoleAssignment assignment,
            String roleName,
            String kind,
            String instance,
            Verification verification
    ) {
        String what = "assignment %s %s".formatted(roleName, kind);

        verification.equal(what + " role", roleName, assignment.roleName());
        verification.equal(what + " kind", kind, assignment.scope().kind());
        verification.equal(what + " instance", instance, assignment.scope().instance());
    }

    private static void verifyGrants(List<PolicyGrant> grants, Verification verification) {
        if (!verification.size("grants", 7, grants)) {
            return;
        }

        verifyGrant(grants.get(0), "form:write", "SELF", null, PolicyEffect.DENY, null, verification);
        verifyGrant(grants.get(1), "form:write", "SCOPE", "${application.defaultInstance}",
                PolicyEffect.ALLOW, null, verification);
        verifyGrant(grants.get(2), "form:*", "SCOPE", "*", PolicyEffect.ALLOW, null, verification);
        verifyGrant(grants.get(3), "form:read", "SCOPE", "instance", PolicyEffect.ALLOW, null, verification);
        verifyGrant(grants.get(4), "form:delete", "SCOPE", "instance", PolicyEffect.DENY,
                "resource.status == 'DRAFT'", verification);
        // The condition made entirely of this grammar's keywords — see the fixture.
        verifyGrant(grants.get(5), "form:share", "SCOPE", "instance", PolicyEffect.DENY,
                "caller.plan != null", verification);
        // ⚠️ Written across three lines and read back as one. The braces are a delimiter and nothing
        // more: `when x and y` and `when { x and y }` produce the same condition, the same source()
        // and therefore the same line quoted back in a refusal.
        verifyGrant(grants.get(6), "entry:read", "SCOPE", "instance", PolicyEffect.DENY,
                "action == 'entry.listByPurpose' and purpose != 'HOLDER'", verification);
    }

    private static void verifyGrant(
            PolicyGrant grant,
            String permission,
            String kind,
            String instance,
            PolicyEffect effect,
            String condition,
            Verification verification
    ) {
        String what = "grant @%s %s".formatted(kind, permission);

        verification.equal(what, permission, grant.permission());
        verification.equal(what + " kind", kind, grant.scope().kind());
        verification.equal(what + " instance", instance, grant.scope().instance());
        verification.equal(what + " effect", effect, grant.effect());
        verification.equal(what + " condition", condition, grant.condition());
    }

    /**
     * Writes the file back out, reads it again, and checks it still says the same thing.
     *
     * @param evaluator    the parser under test
     * @param verification where to record what held and what did not
     */
    private static void verifyRoundTrip(ExpressionEvaluator evaluator, Verification verification) {
        String rewritten = evaluator.rewrite(DECLARATION);

        verifyPolicy(evaluator.parse(rewritten), verification.section("as rewritten"));

        verification.equal("rewriting a rewritten file changes nothing",
                rewritten, evaluator.rewrite(rewritten));
    }

    /**
     * Collects what held and what did not, so one run reports every problem rather than the first.
     */
    public static final class Verification {

        private String section = "";
        private int    checked;
        private int    failed;

        /**
         * Names what the checks that follow are about.
         *
         * @param section the heading to report them under
         * @return this, so a caller can pass it straight on
         */
        public Verification section(String section) {
            this.section = section;
            return this;
        }

        /**
         * Records whether a value is what it should be.
         *
         * @param what     what is being checked, phrased for whoever reads the report
         * @param expected the value the file says
         * @param actual   the value the parser produced
         * @return {@code true} when they match
         */
        public boolean equal(String what, Object expected, Object actual) {
            boolean holds = Objects.equals(expected, actual);

            checked++;

            if (!holds) {
                failed++;
                System.out.printf("  ✗ [%s] %s: expected '%s', got '%s'%n", section, what, expected, actual);
            }

            return holds;
        }

        /**
         * Records whether a list holds as many entries as the file declared.
         *
         * @param what     what the list holds
         * @param expected how many entries there should be
         * @param actual   the list the parser produced
         * @return {@code true} when the count matches, so the caller may look inside
         */
        public boolean size(String what, int expected, List<?> actual) {
            return equal(what + " count", expected, actual.size());
        }

        /**
         * Prints the outcome, and leaves a failing exit code behind for whoever is scripting this.
         */
        public void report() {
            if (failed == 0) {
                System.out.printf("%d checks, all held%n", checked);
                return;
            }

            System.out.printf("%d checks, %d failed%n", checked, failed);
            System.exit(1);
        }
    }
}
