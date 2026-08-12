package org.jmouse.access.el;

import org.jmouse.access.AccessTarget;
import org.jmouse.access.AxisKind;
import org.jmouse.access.EffectivePermissions;
import org.jmouse.access.EffectivePermissionsResolver;
import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.RefusalReason;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeNature;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Subject;
import org.jmouse.access.axis.AccessAxisEvaluator;
import org.jmouse.access.axis.ConditionAxis;
import org.jmouse.access.el.Smoke.Verification;
import org.jmouse.access.el.condition.ExpressionConditionCompiler;
import org.jmouse.access.el.loader.PolicyLoader;
import org.jmouse.access.el.loader.PolicySource;
import org.jmouse.access.el.loader.PolicySources;
import org.jmouse.access.policy.AccessPolicy;
import org.jmouse.access.policy.ConditionCompiler;
import org.jmouse.access.policy.DeclaredScope;
import org.jmouse.access.PlaceholderResolver;
import org.jmouse.access.policy.PolicyBinder;
import org.jmouse.access.policy.PolicyDocuments;
import org.jmouse.access.policy.PolicyException;
import org.jmouse.access.policy.PolicyGrantStore;
import org.jmouse.access.policy.PolicyProjector;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.GrantAttribution;
import org.jmouse.access.spi.GrantOrigin;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.ResolutionCache;
import org.jmouse.access.spi.RoleGrant;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checks the two directions {@link Smoke} does not: a policy <em>written</em> out of a document that
 * was never a file, and several files <em>loaded</em> as one.
 *
 * <p>The property under test is one sentence: <strong>a document written out and read back is the
 * same document.</strong> Everything the control room is about to be built on rests on it — a form
 * that edits a document, a code pane that is the document written out, and a projection of the
 * database shown in the same notation are three views of one object only while that holds.
 *
 * <p>⚠️ Positions are excluded from the comparison and nothing else is. A parsed document points at
 * lines and a projected one points nowhere, so comparing them whole answers "different" about two
 * policies that say exactly the same thing — see {@link PolicyDocuments#withoutSourcePositions}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ExchangeSmoke {

    private static final ScopeKind INSTALLATION = new DeclaredScope("INSTALLATION", 0, ScopeNature.EVERYTHING, null);
    private static final ScopeKind SPACE        = new DeclaredScope("SPACE", 1, ScopeNature.PLACE, "spaceId");
    private static final ScopeKind SELF         = new DeclaredScope("SELF", 2, ScopeNature.OWN_ROWS, null);

    private ExchangeSmoke() {
    }

    public static void main(String[] arguments) {
        ExpressionEvaluator evaluator    = new ExpressionEvaluator();
        Verification        verification = new Verification();

        verifyWritesWhatItRead(evaluator, verification.section("write"));
        verifyPermissionsAProductActuallyHas(evaluator, verification.section("permission shapes"));
        verifyProjectsATable(evaluator, verification.section("project"));
        verifyLoadsSeveralFiles(evaluator, verification.section("load"));
        verifyRefusesACircle(verification.section("include cycle"));
        verifyAttributesEveryFile(evaluator, verification.section("provenance"));
        verifyConditionsNarrowAndNeverWiden(evaluator, verification.section("conditions"));

        verification.report();
    }

    // ── The writer ────────────────────────────────────────────────────────────

    /**
     * Writes a parsed policy out and reads it back.
     *
     * <p>Three checks, and the third is the one that matters to a diff: the document survives, the
     * text is stable, and {@link Smoke}'s field-by-field reading of the fixture still holds against
     * what the writer produced — so this is not merely "something round-tripped", it is every
     * construction the language has.
     */
    private static void verifyWritesWhatItRead(ExpressionEvaluator evaluator, Verification verification) {
        PolicyDocument document = evaluator.parse(Smoke.DECLARATION);
        String         written  = PolicyWriter.write(document);

        verification.equal("a written document parses back to itself",
                withoutPositions(document), withoutPositions(evaluator.parse(written)));

        verification.equal("writing what was written changes nothing",
                written, PolicyWriter.write(evaluator.parse(written)));

        Smoke.verifyPolicy(evaluator.parse(written), verification);
        verification.section("write");
    }

    /**
     * Two shapes a real installation's permissions have, and the language refused for a while.
     *
     * <p>⚠️ Both were found by adopting the language rather than by reading it, and both had the same
     * failure: the file would not <em>parse</em>. Not "a permission that fails to bind" — a permission
     * that <strong>cannot be written down</strong>, whose only remedy is renaming a product's
     * vocabulary to suit its configuration format.
     *
     * <ul>
     *   <li><strong>A keyword as a segment.</strong> {@code role:read} is ordinary wherever roles are
     *       administered, and the lexer reads {@code role} as the block keyword.
     *   <li><strong>More than two segments.</strong> {@code form:write:system} and
     *       {@code space:module:restrict} are real, and a grammar stopping at one colon refuses them.
     * </ul>
     */
    private static void verifyPermissionsAProductActuallyHas(
            ExpressionEvaluator evaluator, Verification verification) {

        String source = """
                policy 'shapes' {
                    permissions {
                        role:read              "Read roles"
                        form:write:system      "Edit the forms the product ships with"
                        space:module:restrict  "Withhold a module from a workspace"
                    }
                    role KEEPER {
                        @INSTALLATION  role:read
                        @INSTALLATION  form:write:system
                        @INSTALLATION  form:*
                    }
                    subject 'u-1' {
                        @INSTALLATION  space:module:restrict  deny
                    }
                }
                """;

        PolicyDocument document = evaluator.parse(source);

        verification.equal("a keyword may be a permission's namespace",
                "role:read", document.permissions().getFirst().name());
        verification.equal("a permission may have more than two segments",
                "form:write:system", document.permissions().get(1).name());
        verification.equal("including one whose middle segment is a plain word",
                "space:module:restrict", document.permissions().get(2).name());

        verification.equal("a bundle carries them too",
                List.of("role:read", "form:write:system", "form:*"),
                document.roles().getFirst().bundle().stream()
                        .map(org.jmouse.access.policy.model.PolicyBundleEntry::permission).toList());

        verification.equal("and so does a personal grant",
                "space:module:restrict", document.subjects().getFirst().grants().getFirst().permission());

        verification.equal("all of it survives being written back out",
                withoutPositions(document), withoutPositions(evaluator.parse(PolicyWriter.write(document))));
    }

    // ── The projector ─────────────────────────────────────────────────────────

    /**
     * Projects a grant store into a policy, and checks the notation says what the store did.
     *
     * <p>⚠️ The denial is the check worth having. A file cannot delete a row, so overriding a stored
     * grant means denying it — and a denial that rendered as a missing line would be a policy whose
     * reader cannot see why a power vanished.
     */
    private static void verifyProjectsATable(ExpressionEvaluator evaluator, Verification verification) {
        PolicyDocument projected = PolicyProjector.project("derived", storedGrants(), List.of("u-42"));
        String         written   = PolicyWriter.write(projected);

        verification.equal("a projection is written and read back unchanged",
                withoutPositions(projected), withoutPositions(evaluator.parse(written, "derived")));

        verification.equal("the role the store answered with is declared", 1, projected.roles().size());
        verification.equal("its bundle is the union of what every store carries",
                2, projected.roles().getFirst().bundle().size());
        verification.equal("a stored denial is written as one",
                true, written.contains("@SELF form:write deny"));
        verification.equal("a stored assignment is written as one",
                true, written.contains("grants SPACE_ADMIN @SPACE:kyiv"));
        verification.equal("and two stores reporting it do not write it twice",
                1, projected.subjects().getFirst().roles().size());
    }

    /**
     * A store answering as a database would: one role held in one workspace, one personal denial.
     *
     * <p>The role is answered twice with different bundles, which is what a composite of a policy and
     * a table looks like part-way through a migration. The projection takes the union, because that is
     * what the engine resolves.
     */
    private static GrantStore storedGrants() {
        return new StubGrantStore(
                List.of(
                        new RoleGrant("SPACE_ADMIN", ScopeReference.of(SPACE, "kyiv"),
                                List.of(new BundledPermission("space:write", SPACE)),
                                GrantAttribution.stored("petro", LocalDateTime.now())),
                        new RoleGrant("SPACE_ADMIN", ScopeReference.of(SPACE, "kyiv"),
                                List.of(new BundledPermission("form:read", INSTALLATION)),
                                GrantAttribution.stored("policy:roles", LocalDateTime.now()))),
                List.of(
                        new DirectGrant("form:write", false, ScopeReference.of(SELF, ScopeKind.NO_INSTANCE),
                                GrantAttribution.stored("petro", "left the team", LocalDateTime.now()))));
    }

    // ── The loader ────────────────────────────────────────────────────────────

    /**
     * Loads three files where one includes another, and checks all of it arrived once.
     *
     * <p>⚠️ The repeated include is the check worth having: {@code shared.jmp} is reached from both
     * other files, and reading it twice would declare its role twice — which the binder refuses,
     * correctly, and about entirely the wrong thing.
     */
    private static void verifyLoadsSeveralFiles(ExpressionEvaluator evaluator, Verification verification) {
        PolicySources sources = library(Map.of(
                "file:/policy/bootstrap.jmp", """
                        include 'shared.jmp'
                        role INSTALLATION_OWNER {
                            @INSTALLATION user:manage
                        }
                        """,
                "file:/policy/roles.jmp", """
                        include 'shared.jmp'
                        role SPACE_ADMIN {
                            @SPACE space:write
                        }
                        """,
                "file:/policy/shared.jmp", """
                        role VIEWER {
                            @SPACE form:read
                        }
                        """));

        PolicyDocument loaded = new PolicyLoader(sources, evaluator)
                .load("innoventa", List.of("file:/policy/bootstrap.jmp", "file:/policy/roles.jmp"));

        verification.equal("the merged policy is named by the loader", "innoventa", loaded.name());
        verification.equal("a file two others include is read once", 3, loaded.roles().size());
        verification.equal("what a file composes with is read first",
                "VIEWER", loaded.roles().getFirst().name());
        verification.equal("the merged document carries no include", 0, loaded.includes().size());
        verification.equal("the merged document is writable",
                true, PolicyWriter.write(loaded).contains("role VIEWER"));
    }

    /** Two files including each other have no order to be read in, and saying so beats picking one. */
    private static void verifyRefusesACircle(Verification verification) {
        PolicySources sources = library(Map.of(
                "file:/policy/first.jmp",  "include 'second.jmp'\nrole A { @SPACE form:read }\n",
                "file:/policy/second.jmp", "include 'first.jmp'\nrole B { @SPACE form:read }\n"));

        try {
            new PolicyLoader(sources).load("innoventa", List.of("file:/policy/first.jmp"));
            verification.equal("a circle of includes is refused", true, false);
        } catch (PolicyException refused) {
            verification.equal("a circle of includes is refused",
                    true, refused.getMessage().contains("include each other"));
            verification.equal("and the whole loop is named, not the file that noticed",
                    true, refused.getMessage().contains("first.jmp")
                          && refused.getMessage().contains("second.jmp"));
        }
    }

    // ── Provenance ────────────────────────────────────────────────────────────

    /**
     * Which <em>file</em> granted this — the question a merge is where you lose the answer to.
     *
     * <p>Before the merge each document knows its own name; after it every declaration belongs to one
     * document called something else. So the merge attributes each declaration as it copies it, and a
     * bound grant can say {@code bootstrap:3} rather than {@code policy:innoventa} — which is the
     * difference between a control room that ends at a file somebody can open and one that ends at a
     * table that does not hold the row.
     */
    private static void verifyAttributesEveryFile(ExpressionEvaluator evaluator, Verification verification) {
        AccessPolicy policy = boundPolicy(evaluator, new ExpressionConditionCompiler());

        GrantOrigin fromBootstrap = policy.subjects().get("u-1").roles().getFirst().attribution().origin();
        GrantOrigin fromRoles     = policy.subjects().get("u-2").grants().getFirst().attribution().origin();

        verification.equal("an assignment names the file it was written in",
                "bootstrap", fromBootstrap.document());
        verification.equal("and the line", 5, fromBootstrap.line());
        verification.equal("a personal grant names its file too", "roles", fromRoles.document());
        verification.equal("a declared grant is not editable as a row",
                false, fromRoles.isEditableAsARow());
        verification.equal("and a stored one is", true, GrantOrigin.stored().isEditableAsARow());
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    /**
     * The sixth axis: conditions read after the permission axis, able only to take away.
     *
     * <p>Four checks, and the third is the one the whole design turns on.
     *
     * <ol>
     *   <li>A conditional deny is <strong>not</strong> a deny in the resolved set — recorded there it
     *       would take the permission away for every row rather than for the ones the rule is about.
     *   <li>It refuses once its condition holds.
     *   <li>⚠️ A conditional <em>allow</em> that fails does not take away a permission somebody also
     *       holds unconditionally. An axis refusing on "some condition failed" would turn a rule that
     *       gives into a rule that takes, which is the one thing narrowing must make impossible.
     *   <li>A permission held <em>only</em> conditionally is refused when its condition fails.
     * </ol>
     */
    private static void verifyConditionsNarrowAndNeverWiden(
            ExpressionEvaluator evaluator, Verification verification) {

        AccessPolicy policy = boundPolicy(evaluator, new ExpressionConditionCompiler());
        GrantStore   store  = new PolicyGrantStore(policy);

        EffectivePermissionsResolver resolver = new EffectivePermissionsResolver(
                store, VOCABULARY, ResolutionCache.none(), null);

        EffectivePermissions resolved = resolver.resolve(
                Subject.of("u-2", "u-2"), AccessTarget.installation().at(SPACE, "kyiv"));

        verification.equal("a conditional deny does not remove from the set",
                true, resolved.contains("form:delete"));
        verification.equal("it is carried as a narrowing instead",
                1, resolved.provenanceOf("form:delete").narrowedBy().size());

        AccessAxisEvaluator axis = new ConditionAxis(
                CONDITION, resolver, RefusedByCondition.CONDITION_NOT_MET,
                (target, place) -> target.place(SPACE).map(Draft::new).orElse(null));

        verification.equal("the denial refuses once its condition holds",
                true, axis.evaluate(Subject.of("u-2", "u-2"),
                        "form:delete", AccessTarget.installation().at(SPACE, "kyiv")).refused());

        verification.equal("and allows where it does not",
                false, axis.evaluate(Subject.of("u-2", "u-2"),
                        "form:delete", AccessTarget.installation().at(SPACE, "lviv")).refused());

        verification.equal("a failing conditional allow never removes an unconditional route",
                false, axis.evaluate(Subject.of("u-2", "u-2"),
                        "form:read", AccessTarget.installation().at(SPACE, "lviv")).refused());

        verification.equal("but a permission held only conditionally is refused when it fails",
                true, axis.evaluate(Subject.of("u-2", "u-2"),
                        "form:share", AccessTarget.installation().at(SPACE, "lviv")).refused());
    }

    /** The row a condition talks about, and the only thing these fixtures ask about one. */
    public static final class Draft {

        private final String space;

        Draft(String space) {
            this.space = space;
        }

        public String getStatus() {
            return "kyiv".equals(space) ? "DRAFT" : "PUBLISHED";
        }
    }

    /** One refusal, so the axis has something to call a no. */
    private enum RefusedByCondition implements RefusalReason {

        CONDITION_NOT_MET;

        @Override
        public AxisKind axis() {
            return CONDITION;
        }

        @Override
        public String title() {
            return "Not for this one";
        }
    }

    /** One axis, declared last — a condition reads what the permission axis resolved. */
    private enum ConditionQuestion implements AxisKind {

        CONDITION;

        @Override
        public int order() {
            return 100;
        }

        @Override
        public boolean required() {
            return false;
        }
    }

    /** Two files, so the merge has something to attribute, bound against a fixed vocabulary. */
    private static AccessPolicy boundPolicy(ExpressionEvaluator evaluator, ConditionCompiler compiler) {
        PolicySources sources = library(Map.of(
                "file:/policy/bootstrap.jmp", """
                        role KEEPER {
                            @SPACE  form:read
                        }
                        subject 'u-1' {
                            grants KEEPER @SPACE:kyiv
                        }
                        """,
                "file:/policy/roles.jmp", """
                        subject 'u-2' {
                            @SPACE:kyiv  form:delete
                            @SPACE:kyiv  form:delete  deny  when resource.status == 'DRAFT'
                            @SPACE:lviv  form:delete
                            @SPACE:lviv  form:delete  deny  when resource.status == 'DRAFT'
                            @SPACE:lviv  form:read
                            @SPACE:lviv  form:read    allow when resource.status == 'DRAFT'
                            @SPACE:lviv  form:share   allow when resource.status == 'DRAFT'
                        }
                        """));

        PolicyDocument document = new PolicyLoader(sources, evaluator).load(
                "innoventa", List.of("file:/policy/bootstrap.jmp", "file:/policy/roles.jmp"));

        return new PolicyBinder(VOCABULARY, CATALOGUE, compiler, PlaceholderResolver.none())
                .bind(document);
    }

    private static final ScopeCatalog VOCABULARY = new ScopeCatalog(
            List.of(INSTALLATION, SPACE, SELF));

    private static final PermissionCatalog CATALOGUE = new PermissionCatalog(
            List.of("form:read", "form:write", "form:delete", "form:share", "space:write"));

    private static final AxisKind CONDITION = ConditionQuestion.CONDITION;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static PolicyDocument withoutPositions(PolicyDocument document) {
        return PolicyDocuments.withoutSourcePositions(document);
    }

    /** Files by location, with includes resolved beside the file that wrote them. */
    private static PolicySources library(Map<String, String> files) {
        Map<String, String> byLocation = new LinkedHashMap<>(files);

        return new PolicySources() {

            @Override
            public List<PolicySource> at(String location) {
                String text = byLocation.get(location);
                return text == null ? List.of() : List.of(PolicySource.at(location, text));
            }

            @Override
            public Optional<PolicySource> included(String path, PolicySource from) {
                String beside = from.location().substring(0, from.location().lastIndexOf('/') + 1) + path;
                return Optional.ofNullable(byLocation.get(beside))
                        .map(text -> PolicySource.at(beside, text));
            }
        };
    }

    /** A store that answers the same thing however it is asked — enough to project. */
    private record StubGrantStore(List<RoleGrant> roles, List<DirectGrant> grants) implements GrantStore {

        @Override
        public List<RoleGrant> rolesCovering(String subjectId, List<ScopeReference> chain) {
            return roles;
        }

        @Override
        public List<DirectGrant> directCovering(String subjectId, List<ScopeReference> chain) {
            return grants;
        }

        @Override
        public List<RoleGrant> rolesHeldBy(String subjectId) {
            return roles;
        }

        @Override
        public List<DirectGrant> directHeldBy(String subjectId) {
            return grants;
        }
    }
}
