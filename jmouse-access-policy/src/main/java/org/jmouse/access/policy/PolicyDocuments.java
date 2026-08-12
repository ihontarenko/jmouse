package org.jmouse.access.policy;

import org.jmouse.access.policy.model.PolicyBundleEntry;
import org.jmouse.access.policy.model.PolicyCapabilityDeclaration;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyEntitlement;
import org.jmouse.access.policy.model.PolicyGrant;
import org.jmouse.access.policy.model.PolicyInclude;
import org.jmouse.access.policy.model.PolicyActionDeclaration;
import org.jmouse.access.policy.model.PolicyPermissionDeclaration;
import org.jmouse.access.policy.model.PolicyPlan;
import org.jmouse.access.policy.model.PolicyPlanGrant;
import org.jmouse.access.policy.model.PolicyRole;
import org.jmouse.access.policy.model.PolicyRoleAssignment;
import org.jmouse.access.policy.model.PolicyScopeDeclaration;
import org.jmouse.access.policy.model.PolicySubject;
import org.jmouse.access.policy.model.SourceSpan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Operations on whole documents — the two a loader and a control room both need, and neither of which
 * belongs on the record itself.
 *
 * <p>{@link PolicyDocument} is the exchange format in both directions: a parser produces one, a writer
 * renders one, a projection derives one from tables. What that leaves over is the arithmetic
 * <em>between</em> documents, which is here.
 */
public final class PolicyDocuments {

    private PolicyDocuments() {
    }

    /**
     * Several files read as one document.
     *
     * <p>⚠️ <strong>The merged document carries no {@code include}.</strong> An include is a question
     * about which files take part, and merging is the answer to it; a merged document that still said
     * {@code include 'bootstrap.jmp'} would, written back out and reloaded, hold bootstrap's rules
     * twice.
     *
     * <p>Roles and subjects are concatenated rather than combined. Two files declaring the same role
     * is a real disagreement — {@link PolicyBinder} refuses it, and refusing is right, because which
     * bundle wins would be a question about file order and file order must not decide what anybody may
     * do.
     *
     * @param name      what the merged document is called; every grant reports it as provenance
     * @param documents the parsed files, in load order
     * @throws PolicyException where two files state a vocabulary that does not agree
     */
    public static PolicyDocument merge(String name, List<PolicyDocument> documents) {
        List<PolicyRole>        roles        = new ArrayList<>();
        List<PolicySubject>     subjects     = new ArrayList<>();
        List<PolicyPlan>        plans        = new ArrayList<>();
        List<PolicyEntitlement> entitlements = new ArrayList<>();

        for (PolicyDocument document : documents) {
            /*
             * Plans and entitlements concatenate, like roles and subjects and for the same reason.
             * Two files declaring one plan code is a real disagreement rather than something to
             * resolve by file order; binding refuses it. Two files entitling the same subject is not
             * a disagreement at all — deny wins over both, wherever either was written.
             */
            plans.addAll(document.plans());
            entitlements.addAll(document.entitlements());

            // ⚠️ Attributed here or nowhere. Before the merge each document knows its own name; after
            // it every declaration belongs to one document with one name, and "which file grants
            // this" — the question the control room exists to answer — has no answer left.
            roles.addAll(document.roles().stream()
                    .map(role -> writtenIn(role, document.name()))
                    .toList());
            subjects.addAll(document.subjects().stream()
                    .map(subject -> writtenIn(subject, document.name()))
                    .toList());
        }

        return new PolicyDocument(
                name,
                List.of(),
                mergeScopes(documents),
                mergePermissions(documents),
                mergeActions(documents),
                mergeCapabilities(documents),
                roles,
                plans,
                subjects,
                entitlements);
    }

    /**
     * The same document with every position forgotten.
     *
     * <p>What it is for: comparing a document that was <em>parsed</em> with one that was
     * <em>built</em>. A projected policy has no file to point at and a rewritten one points at
     * different columns, so a straight record comparison of the two answers "different" about two
     * documents saying exactly the same thing. Everything a policy <em>means</em> survives this;
     * only the ability to name a line does not.
     *
     * @param document the document to flatten
     * @return the same declarations, each anchored nowhere
     */
    public static PolicyDocument withoutSourcePositions(PolicyDocument document) {
        return new PolicyDocument(
                document.name(),
                document.includes().stream()
                        .map(include -> new PolicyInclude(include.path(), SourceSpan.none()))
                        .toList(),
                document.scopes().stream()
                        .map(scope -> new PolicyScopeDeclaration(
                                scope.name(), scope.nature(), scope.parameter(), SourceSpan.none()))
                        .toList(),
                document.permissions().stream()
                        .map(permission -> new PolicyPermissionDeclaration(
                                permission.name(), permission.description(), SourceSpan.none()))
                        .toList(),
                document.actions().stream()
                        .map(action -> new PolicyActionDeclaration(
                                action.name(), action.description(), action.values(), SourceSpan.none()))
                        .toList(),
                document.capabilities().stream()
                        .map(capability -> new PolicyCapabilityDeclaration(
                                capability.key(), capability.displayName(), capability.kind(),
                                capability.scopes(), capability.paid(), SourceSpan.none()))
                        .toList(),
                document.roles().stream()
                        .map(PolicyDocuments::withoutSourcePositions)
                        .toList(),
                document.plans().stream()
                        .map(PolicyDocuments::withoutSourcePositions)
                        .toList(),
                document.subjects().stream()
                        .map(PolicyDocuments::withoutSourcePositions)
                        .toList(),
                document.entitlements().stream()
                        .map(entitlement -> new PolicyEntitlement(
                                entitlement.at(), entitlement.kind(), entitlement.subject(),
                                entitlement.quantity(), entitlement.unlimited(), entitlement.from(),
                                entitlement.until(), entitlement.reason(), SourceSpan.none()))
                        .toList());
    }

    private static PolicyPlan withoutSourcePositions(PolicyPlan plan) {
        return new PolicyPlan(
                plan.code(),
                plan.displayName(),
                plan.order(),
                plan.note(),
                plan.extendsCode(),
                plan.grants().stream()
                        .map(grant -> new PolicyPlanGrant(
                                grant.capability(), grant.quantity(), grant.period(),
                                grant.unlimited(), SourceSpan.none()))
                        .toList(),
                SourceSpan.none());
    }

    // ── Vocabulary ────────────────────────────────────────────────────────────

    /**
     * The scopes the load declares, where the files agree about them.
     *
     * <p>⚠️ <strong>Declaration order is width order, so a partial statement of it is not a statement
     * at all.</strong> Two files each naming some of the floors do not between them name an order:
     * merging them by first-occurrence would produce a covering chain neither file describes, and
     * nobody reading either one would be able to predict it. So one file may state the vocabulary, or
     * several may state precisely the same one.
     */
    private static List<PolicyScopeDeclaration> mergeScopes(List<PolicyDocument> documents) {
        List<PolicyDocument> stating = documents.stream()
                .filter(document -> !document.scopes().isEmpty())
                .toList();

        if (stating.isEmpty()) {
            return List.of();
        }

        PolicyDocument first = stating.getFirst();

        for (PolicyDocument other : stating) {
            if (!statesTheSameScopes(first, other)) {
                throw new PolicyException(
                        "'" + first.name() + "' and '" + other.name() + "' both declare scopes, and "
                        + "they do not declare the same ones in the same order. Declaration order is "
                        + "width order, so two files each stating part of it state no order at all — "
                        + "keep the scopes block in one file, or write it identically in both.");
            }
        }

        return first.scopes();
    }

    private static boolean statesTheSameScopes(PolicyDocument first, PolicyDocument other) {
        return withoutSourcePositions(first).scopes().equals(withoutSourcePositions(other).scopes());
    }

    /**
     * Every permission the load declares, by name.
     *
     * <p>Unlike scopes these carry no order, so several files may each declare their own — which is
     * what lets a product's modules ship their permissions beside themselves. What they may not do is
     * describe the same permission differently: a catalogue with two answers is a catalogue an
     * administration screen picks one of at random.
     */
    private static List<PolicyPermissionDeclaration> mergePermissions(List<PolicyDocument> documents) {
        Map<String, PolicyPermissionDeclaration> declared = new LinkedHashMap<>();
        Map<String, String>                      declaredBy = new LinkedHashMap<>();

        for (PolicyDocument document : documents) {
            for (PolicyPermissionDeclaration permission : document.permissions()) {
                PolicyPermissionDeclaration existing = declared.get(permission.name());

                if (existing == null) {
                    declared.put(permission.name(), permission);
                    declaredBy.put(permission.name(), document.name());
                    continue;
                }

                if (!Objects.equals(existing.description(), permission.description())) {
                    throw new PolicyException(
                            "'" + permission.name() + "' is declared in '" + declaredBy.get(permission.name())
                            + "' and again in '" + document.name() + "', with different descriptions. A "
                            + "permission means one thing, and a catalogue holding two answers is one an "
                            + "administration screen chooses between at random.");
                }
            }
        }

        return List.copyOf(declared.values());
    }

    /**
     * Every action the load declares, by name.
     *
     * <p>Merged the way permissions are and refused for the same reason. Two files describing one
     * action differently is a catalogue with two answers; two files disagreeing about what an action
     * <em>publishes</em> is worse, because it is what a rule is checked against — one of the two
     * spellings would silently make a rule legal and the other would make it a boot failure,
     * depending on load order.
     */
    private static List<PolicyActionDeclaration> mergeActions(List<PolicyDocument> documents) {
        Map<String, PolicyActionDeclaration> declared   = new LinkedHashMap<>();
        Map<String, String>                  declaredBy = new LinkedHashMap<>();

        for (PolicyDocument document : documents) {
            for (PolicyActionDeclaration action : document.actions()) {
                PolicyActionDeclaration existing = declared.get(action.name());

                if (existing == null) {
                    declared.put(action.name(), action);
                    declaredBy.put(action.name(), document.name());
                    continue;
                }

                if (!Objects.equals(existing.description(), action.description())
                    || !Objects.equals(existing.values(), action.values())) {

                    throw new PolicyException(
                            "'" + action.name() + "' is declared in '" + declaredBy.get(action.name())
                            + "' and again in '" + document.name() + "', and the two do not agree. An "
                            + "action means one thing and publishes one set of values; whichever the "
                            + "load happened to read first would decide which rules about it are legal.");
                }
            }
        }

        return List.copyOf(declared.values());
    }

    /**
     * The capabilities the load declares, where the files agree about them.
     *
     * <p>Merged the way permissions are rather than the way scopes are: these carry no order, so
     * several files may each declare their own and a product's features can ship their capabilities
     * beside themselves.
     *
     * <p>⚠️ What they may not do is describe the same key <em>differently</em>. A capability declared
     * as a {@code limit} in one file and a {@code quota} in another is not a merge conflict to
     * resolve — it is two incompatible statements about whether the number can be recounted, and
     * picking one would make the other file's reader wrong without telling them.
     */
    private static List<PolicyCapabilityDeclaration> mergeCapabilities(List<PolicyDocument> documents) {
        Map<String, PolicyCapabilityDeclaration> declared   = new LinkedHashMap<>();
        Map<String, String>                      declaredBy = new LinkedHashMap<>();

        for (PolicyDocument document : documents) {
            for (PolicyCapabilityDeclaration capability : document.capabilities()) {
                PolicyCapabilityDeclaration existing = declared.get(capability.key());

                if (existing == null) {
                    declared.put(capability.key(), capability);
                    declaredBy.put(capability.key(), document.name());
                    continue;
                }

                if (!statesTheSameCapability(existing, capability)) {
                    throw new PolicyException(
                            "'" + capability.key() + "' is declared in '" + declaredBy.get(capability.key())
                            + "' and again in '" + document.name() + "', and the two do not agree. A "
                            + "capability has one shape and one set of places it may be granted at; two "
                            + "answers means whichever file loaded last decides what a plan containing "
                            + "it actually gives.");
                }
            }
        }

        return List.copyOf(declared.values());
    }

    private static boolean statesTheSameCapability(
            PolicyCapabilityDeclaration first, PolicyCapabilityDeclaration other) {

        return Objects.equals(first.kind(), other.kind())
                && Objects.equals(first.scopes(), other.scopes())
                && first.paid() == other.paid()
                && Objects.equals(first.displayName(), other.displayName());
    }

    // ── Attribution ───────────────────────────────────────────────────────────

    private static PolicyRole writtenIn(PolicyRole role, String document) {
        return new PolicyRole(
                role.name(),
                role.bundle().stream()
                        .map(entry -> new PolicyBundleEntry(
                                entry.permission(), entry.scope(), attributed(entry.at(), document)))
                        .toList(),
                attributed(role.at(), document));
    }

    private static PolicySubject writtenIn(PolicySubject subject, String document) {
        return new PolicySubject(
                subject.id(),
                subject.roles().stream()
                        .map(assignment -> new PolicyRoleAssignment(
                                assignment.roleName(), assignment.scope(),
                                attributed(assignment.at(), document)))
                        .toList(),
                subject.grants().stream()
                        .map(grant -> new PolicyGrant(
                                grant.permission(), grant.scope(), grant.effect(), grant.condition(),
                                attributed(grant.at(), document)))
                        .toList(),
                attributed(subject.at(), document));
    }

    /**
     * A declaration's origin — the one it already knows, or this document where it knows none.
     *
     * <p>⚠️ <strong>A merge of merges must not relabel.</strong> Merging is not always the last thing
     * that happens to a document: a loader merges the files, and an application that composes a second
     * document on top of them merges again. Overwriting the name each time would attribute every
     * declaration to the outermost merge, so a grant written in {@code bootstrap.jmp} would report the
     * whole policy as its origin and the control room's "which file grants this" would have one
     * answer for everything.
     */
    private static SourceSpan attributed(SourceSpan span, String document) {
        return span.namesADocument() ? span : span.in(document);
    }

    // ── Positions ─────────────────────────────────────────────────────────────

    private static PolicyRole withoutSourcePositions(PolicyRole role) {
        return new PolicyRole(
                role.name(),
                role.bundle().stream()
                        .map(entry -> new PolicyBundleEntry(
                                entry.permission(), entry.scope(), SourceSpan.none()))
                        .toList(),
                SourceSpan.none());
    }

    private static PolicySubject withoutSourcePositions(PolicySubject subject) {
        return new PolicySubject(
                subject.id(),
                subject.roles().stream()
                        .map(assignment -> new PolicyRoleAssignment(
                                assignment.roleName(), assignment.scope(), SourceSpan.none()))
                        .toList(),
                subject.grants().stream()
                        .map(grant -> new PolicyGrant(
                                grant.permission(), grant.scope(), grant.effect(), grant.condition(),
                                SourceSpan.none()))
                        .toList(),
                SourceSpan.none());
    }
}
