package org.jmouse.access.policy;

import org.jmouse.access.PermissionCatalog;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeNature;
import org.jmouse.access.policy.model.PolicyDocument;
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

    private PolicyVocabulary() {
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

        if (!document.declaresVocabulary()) {
            return;
        }

        List<PolicyProblem> problems = new ArrayList<>();

        checkScopes(document, scopes, problems);
        checkPermissions(document, permissions, problems);
        refuse(document, problems);
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

    private static void refuse(PolicyDocument document, List<PolicyProblem> problems) {
        if (!problems.isEmpty()) {
            throw new PolicyException(
                    "The vocabulary of '" + document.name() + "' disagrees with this installation:\n  - "
                    + problems.stream().map(PolicyProblem::toString).collect(joining("\n  - ")),
                    problems);
        }
    }
}
