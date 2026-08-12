package org.jmouse.access.policy;

import org.jmouse.access.ActionCatalog;
import org.jmouse.access.CapabilityCatalog;
import org.jmouse.access.CapabilityDefinition;
import org.jmouse.access.CapabilityKind;
import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeNature;
import org.jmouse.access.policy.model.PolicyCapabilityDeclaration;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyActionDeclaration;
import org.jmouse.access.policy.model.PolicyPermissionDeclaration;
import org.jmouse.access.policy.model.PolicyScopeDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.util.stream.Collectors.joining;

/**
 * What a {@code scopes { }} / {@code permissions { }} block means — and it means one of two different
 * things depending on who is listening.
 *
 * <h2>Checked against, or built from</h2>
 *
 * <p><strong>An installation that registered its vocabulary in code gets the file checked against
 * it.</strong> This is the valuable mode and the one Innoventa is in: scopes are an enum because four
 * columns map it {@code @Enumerated(STRING)} and {@code @RequiresAccess} needs a compile-time
 * constant, so a file cannot be their source of truth. What a file <em>can</em> be is a statement of
 * the same fact that fails loudly the day the two disagree — which is the only thing that keeps
 * documentation true.
 *
 * <p><strong>An installation that registered nothing gets its vocabulary built from the file.</strong>
 * {@link ScopeKind} is an interface, so nothing in the engine requires an enum; a product with no
 * mapped columns and no annotations naming scopes can keep its floors in configuration and get
 * {@link DeclaredScope}s.
 *
 * <p>⚠️ <strong>Declaration order is the width order.</strong> There is no rank column, deliberately:
 * a rank written beside each scope states the same fact twice, and the day the two disagree the
 * covering chain reorders without anybody touching it.
 */
public final class PolicyVocabulary {

    private static final Map<String, ScopeNature> NATURES = Map.of(
            "everything", ScopeNature.EVERYTHING,
            "place",      ScopeNature.PLACE,
            "own-rows",   ScopeNature.OWN_ROWS,
            "own_rows",   ScopeNature.OWN_ROWS);

    /** The words a {@code capabilities} block writes, and the shapes they mean. */
    private static final Map<String, CapabilityKind> KINDS = Map.of(
            "gate",  CapabilityKind.GATE,
            "limit", CapabilityKind.LIMIT,
            "quota", CapabilityKind.QUOTA);

    private PolicyVocabulary() {
    }

    /**
     * The capabilities a document declares, as a catalogue.
     *
     * <p>The <em>built-from</em> mode, for a product that states its vocabulary in the file rather
     * than in code — the same choice {@link #scopesOf} offers for floors.
     *
     * @throws PolicyException where a kind is not one of the three
     */
    public static CapabilityCatalog capabilitiesOf(PolicyDocument document) {
        List<CapabilityDefinition> definitions = new ArrayList<>();

        for (PolicyCapabilityDeclaration declared : document.capabilities()) {
            CapabilityKind kind = KINDS.get(declared.kind());

            if (kind == null) {
                throw new PolicyException(
                        "'" + declared.key() + "' is declared '" + declared.kind() + "', which is not "
                        + "a shape a capability can have. Write 'gate' for something that is open or "
                        + "closed, 'limit' for a standing count, or 'quota' for something consumed "
                        + "over a period.");
            }

            definitions.add(new CapabilityDefinition(
                    declared.key(), declared.displayName(), kind, declared.scopes(), declared.paid()));
        }

        return new CapabilityCatalog(definitions);
    }

    /**
     * The scopes a document declares, as a catalogue.
     *
     * @throws PolicyException where a nature is not one of the three, or the resulting set does not
     *                         hold together — {@link ScopeCatalog} does that checking itself
     */
    public static ScopeCatalog scopesOf(PolicyDocument document) {
        List<ScopeKind>     kinds    = new ArrayList<>();
        List<PolicyProblem> problems = new ArrayList<>();
        int                 rank     = 0;

        for (PolicyScopeDeclaration declared : document.scopes()) {
            ScopeNature nature = NATURES.get(declared.nature());

            if (nature == null) {
                problems.add(PolicyProblem.at(declared.at(), "'" + declared.nature() + "' is not a scope "
                             + "nature. Write one of: everything, place, own-rows."));
                continue;
            }
            if (nature != ScopeNature.PLACE && declared.parameter() != null) {
                problems.add(PolicyProblem.at(declared.at(), "'" + declared.name() + "' is "
                             + declared.nature() + ", which names no instance, so parameter="
                             + declared.parameter() + " could never be read."));
                continue;
            }

            kinds.add(new DeclaredScope(declared.name(), rank++, nature, declared.parameter()));
        }

        refuse(document, problems);

        try {
            return new ScopeCatalog(kinds);
        } catch (IllegalArgumentException malformed) {
            throw new PolicyException(
                    "The scopes block of '" + document.name() + "' does not hold together: "
                    + malformed.getMessage(), malformed);
        }
    }

    /** The permissions a document declares, as a catalogue. */
    public static PermissionCatalog permissionsOf(PolicyDocument document) {
        return new PermissionCatalog(document.permissions().stream()
                .map(PolicyPermissionDeclaration::name)
                .toList());
    }

    /**
     * Checks a document's vocabulary against the one already registered, and complains about every
     * difference.
     *
     * <p>Both directions matter and for different reasons. A scope the file declares and the code does
     * not means the file is describing an installation this is not — grants written against it would
     * be silently unreachable. A scope the code registers and the file omits means the file has
     * stopped being a complete description, which is how documentation becomes a lie one commit at a
     * time.
     *
     * <p>Nature and width are compared too. A file agreeing on the names while disagreeing on the
     * order would be worse than one that disagreed openly: the covering chain follows the code, and
     * the reader follows the file.
     */
    public static void checkAgainst(
            PolicyDocument document, ScopeCatalog scopes, PermissionCatalog permissions) {

        checkAgainst(document, scopes, permissions, CapabilityCatalog.empty());
    }

    /**
     * The same, including the entitlement axis's vocabulary.
     *
     * @param capabilities what the application registers as grantable, or
     *                     {@link CapabilityCatalog#empty()} where it has not adopted the axis
     */
    public static void checkAgainst(
            PolicyDocument document, ScopeCatalog scopes, PermissionCatalog permissions,
            CapabilityCatalog capabilities) {

        checkAgainst(document, scopes, permissions, capabilities, ActionCatalog.empty());
    }

    /**
     * The same, including what this installation's calls say they are doing.
     *
     * @param actions what the application publishes, or {@link ActionCatalog#empty()} where it
     *                publishes nothing — in which case an {@code actions} block goes unchecked, for
     *                the reason an empty capability catalogue does: an installation that registers no
     *                vocabulary has not adopted the axis, and refusing its file would be refusing it
     *                over a feature it is not using
     */
    public static void checkAgainst(
            PolicyDocument document, ScopeCatalog scopes, PermissionCatalog permissions,
            CapabilityCatalog capabilities, ActionCatalog actions) {

        if (!document.declaresVocabulary()) {
            return;
        }

        List<PolicyProblem> problems = new ArrayList<>();

        checkScopes(document, scopes, problems);
        checkPermissions(document, permissions, problems);
        checkActions(document, actions, problems);
        checkCapabilities(document, capabilities, scopes, problems);
        refuse(document, problems);
    }

    /**
     * Checks the action catalogue, and what each action is said to publish.
     *
     * <p>⚠️ <strong>An action written here that no route publishes is the dangerous direction.</strong>
     * A rule scoped to it never fires — and a conditional allow that never holds is a refusal nobody
     * ordered, while a conditional deny that never holds is a door somebody believes is closed. Both
     * are silent, and both are indistinguishable from working.
     *
     * <p>The other direction — a route publishing something the file never mentions — is deliberately
     * <em>not</em> a failure here. A route may legitimately publish a value no rule reads yet, and
     * refusing to boot over one would make adding a declaration a two-repository change. A product
     * that wants to hear about it warns, which is where "not yet used" can be told from "forgotten".
     *
     * <p>The {@code publishes} list is checked strictly, though. It is what an editor offers somebody
     * writing a rule, so a value listed here that never arrives is a rule they will write and nobody
     * will see fail.
     */
    private static void checkActions(
            PolicyDocument document, ActionCatalog actions, List<PolicyProblem> problems) {

        if (actions.publishedValuesByAction().isEmpty()) {
            return;
        }

        for (PolicyActionDeclaration declared : document.actions()) {
            if (!actions.contains(declared.name())) {
                problems.add(PolicyProblem.at(declared.at(), "this file declares an action '"
                             + declared.name() + "' that no route publishes. A rule scoped to it would "
                             + "never fire, and a rule that never fires is indistinguishable from one "
                             + "that works. Known actions: " + actions.all() + "."));
                continue;
            }

            Set<String> published = actions.valuesOf(declared.name());

            for (String value : declared.values()) {
                if (!published.contains(value)) {
                    problems.add(PolicyProblem.at(declared.at(), "'" + declared.name()
                                 + "' is declared as publishing '" + value + "', and it does not. What "
                                 + "it publishes is " + published + "."));
                }
            }
        }
    }

    private static void checkScopes(
            PolicyDocument document, ScopeCatalog scopes, List<PolicyProblem> problems) {

        if (document.scopes().isEmpty()) {
            return;
        }

        List<ScopeKind> registered = scopes.all();
        int             position   = 0;

        for (PolicyScopeDeclaration declared : document.scopes()) {
            ScopeKind matching = scopes.byName(declared.name()).orElse(null);

            if (matching == null) {
                problems.add(PolicyProblem.at(declared.at(), "this file declares a scope '"
                             + declared.name() + "' that the application does not register. Grants "
                             + "written against it could never apply."));
                position++;
                continue;
            }

            ScopeNature expected = NATURES.get(declared.nature());

            if (expected != null && matching.nature() != expected) {
                problems.add(PolicyProblem.at(declared.at(), "'" + declared.name() + "' is declared "
                             + declared.nature() + " here and " + matching.nature()
                             + " in the application."));
            }

            if (position < registered.size() && !registered.get(position).name().equals(declared.name())) {
                problems.add(PolicyProblem.at(declared.at(), "'" + declared.name() + "' is written in "
                             + "position " + (position + 1) + ", where the application has '"
                             + registered.get(position).name() + "'. Declaration order is width order, "
                             + "so this file describes a different covering chain from the one that "
                             + "runs."));
            }

            position++;
        }

        Set<String> written = new TreeSet<>(document.scopes().stream()
                .map(PolicyScopeDeclaration::name).toList());

        registered.stream()
                .map(ScopeKind::name)
                .filter(name -> !written.contains(name))
                .forEach(name -> problems.add(PolicyProblem.anywhere(
                        "the application registers a scope '" + name + "' that this file does not "
                        + "declare, so the file is no longer a complete description of the model.")));
    }

    private static void checkPermissions(
            PolicyDocument document, PermissionCatalog permissions, List<PolicyProblem> problems) {

        for (PolicyPermissionDeclaration declared : document.permissions()) {
            if (!permissions.contains(declared.name())) {
                problems.add(PolicyProblem.at(declared.at(), "this file declares a permission '"
                             + declared.name() + "' that the application does not register."));
            }
        }
    }

    /**
     * Checks the capability catalogue in both directions, and the places each one may be granted at.
     *
     * <p>⚠️ <strong>The second direction is the one that earns its keep.</strong> A capability the
     * code registers and the file never declares is not a harmless omission: bundles are written in
     * this file, so a capability missing from it is one no tier can ever include — the feature ships,
     * nobody can buy it, and nothing says why. That is exactly how a catalogue goes stale one feature
     * at a time.
     *
     * <p>⚠️ A capability whose {@code per} names a scope this installation does not have is refused
     * here rather than at the first grant. A grant addressed at a place that cannot exist would be
     * accepted, stored, and then silently never read.
     */
    private static void checkCapabilities(
            PolicyDocument document, CapabilityCatalog capabilities, ScopeCatalog scopes,
            List<PolicyProblem> problems) {

        if (capabilities.isEmpty()) {
            return;
        }

        Set<String> declared = new TreeSet<>();

        for (PolicyCapabilityDeclaration capability : document.capabilities()) {
            declared.add(capability.key());
            checkOneCapability(capability, capabilities, scopes, problems);
        }

        capabilities.all().stream()
                .filter(key -> !declared.contains(key))
                .forEach(key -> problems.add(PolicyProblem.anywhere(
                        "the application registers a capability '" + key + "' that this file does not "
                        + "declare, so no plan here can ever include it.")));
    }

    private static void checkOneCapability(
            PolicyCapabilityDeclaration capability, CapabilityCatalog capabilities,
            ScopeCatalog scopes, List<PolicyProblem> problems) {

        CapabilityDefinition registered = capabilities.find(capability.key()).orElse(null);

        if (registered == null) {
            problems.add(PolicyProblem.at(capability.at(), "this file declares a capability '"
                         + capability.key() + "' that the application does not register. A plan "
                         + "including it would give nothing."));
            return;
        }

        CapabilityKind expected = KINDS.get(capability.kind());

        if (expected != null && registered.kind() != expected) {
            problems.add(PolicyProblem.at(capability.at(), "'" + capability.key() + "' is declared "
                         + capability.kind() + " here and " + registered.kind() + " in the "
                         + "application. Whether a number can be recounted is not a detail — a limit "
                         + "is answered by counting what exists and a quota is not."));
        }

        capability.scopes().stream()
                .filter(scope -> scopes.byName(scope).isEmpty())
                .forEach(scope -> problems.add(PolicyProblem.at(
                        capability.at(), unknownScope(capability, scope, scopes))));
    }

    /**
     * ⚠️ A scope name is written one way, and {@code per organization} is not it.
     *
     * <p>Matching case-insensitively was the obvious kindness and is the wrong one: it would make
     * {@code ORGANIZATION} and {@code organization} two spellings of one name, so a renamed scope
     * would keep matching and the file would keep looking right. Instead the mismatch is refused and
     * the message says exactly what to type — the failure a reader can fix in one keystroke, rather
     * than a rule they have to be told twice.
     */
    private static String unknownScope(
            PolicyCapabilityDeclaration capability, String written, ScopeCatalog scopes) {

        String message = "'" + capability.key() + "' may be granted 'per " + written + "', which is "
                         + "not a scope this installation has";

        return scopes.all().stream()
                .map(ScopeKind::name)
                .filter(name -> name.equalsIgnoreCase(written))
                .findFirst()
                .map(name -> message + " — it is written '" + name + "' everywhere else, and one name "
                             + "spelled two ways is a rename waiting to go unnoticed.")
                .orElse(message + ".");
    }

    private static void refuse(PolicyDocument document, List<PolicyProblem> problems) {
        if (!problems.isEmpty()) {
            throw new PolicyException(
                    "The vocabulary of '" + document.name() + "' disagrees with this installation:\n  - "
                    + problems.stream().map(PolicyProblem::toString).collect(joining("\n  - ")),
                    problems);
        }
    }
}
