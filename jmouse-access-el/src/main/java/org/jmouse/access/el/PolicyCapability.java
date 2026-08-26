package org.jmouse.access.el;

import org.jmouse.access.el.node.ActionsNode;
import org.jmouse.access.el.node.CapabilitiesNode;
import org.jmouse.access.el.node.EntitlementsNode;
import org.jmouse.access.el.node.IncludeNode;
import org.jmouse.access.el.node.PermissionsNode;
import org.jmouse.access.el.node.PlansNode;
import org.jmouse.access.el.node.RoleNode;
import org.jmouse.access.el.node.ScopesNode;
import org.jmouse.access.el.node.SubjectNode;
import org.jmouse.access.el.node.VariablesNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.translate.Capability;

/**
 * What a {@code .jmp} destination may declare it can honour — one capability per top-level
 * declaration a policy file holds.
 *
 * <h2>⚠️ Why the language's own capabilities do not fit</h2>
 *
 * <p>{@link Capability#FILTER}, {@link Capability#SORT}, {@link Capability#AGGREGATE} and the rest
 * are a <em>query's</em> vocabulary. A policy has no clauses to filter or rows to sort; it has
 * declarations. So every capability here is qualified with {@code policy.}, which is exactly the rule
 * {@link Capability} states for a vocabulary that is not the language's own — and it means the day
 * jMQ mints a {@code plans} capability, nothing collides.</p>
 *
 * <h2>⚠️ One per declaration, not one for the whole file</h2>
 *
 * <p>A single {@code policy.everything} would be a declaration that refuses nothing, which is the
 * shape {@link org.jmouse.el.translate.Capabilities} exists to prevent. Splitting it per block is
 * what lets a narrower destination exist at all: a documentation screen rendering only the vocabulary
 * half declares four of these and is <strong>refused</strong> when handed a file that also assigns
 * grants, rather than quietly publishing a policy with the grants missing.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PolicyCapability {

    /** The namespace every capability here carries — see {@link Capability#isQualified()}. */
    public static final String NAMESPACE = "policy";

    /** {@code include "…"} — composing a file out of others. */
    public static final Capability INCLUDE = Capability.named(NAMESPACE, "include");

    /** {@code declare scopes { … }} — where a permission may be held. */
    public static final Capability SCOPES = Capability.named(NAMESPACE, "scopes");

    /** {@code declare permissions { … }} — what may be held. */
    public static final Capability PERMISSIONS = Capability.named(NAMESPACE, "permissions");

    /** {@code declare actions { … }} — what a permission lets somebody do. */
    public static final Capability ACTIONS = Capability.named(NAMESPACE, "actions");

    /** {@code declare variables { … }} — the names a condition may read. */
    public static final Capability VARIABLES = Capability.named(NAMESPACE, "variables");

    /** {@code declare capabilities { … }} — metered things, and which of them are paid. */
    public static final Capability CAPABILITIES = Capability.named(NAMESPACE, "capabilities");

    /** {@code declare role NAME { … }} — a bundle of permissions. */
    public static final Capability ROLES = Capability.named(NAMESPACE, "roles");

    /** {@code declare plans { … }} — what a plan grants, and how much of it. */
    public static final Capability PLANS = Capability.named(NAMESPACE, "plans");

    /** {@code assign subject "…" { … }} — who holds what. */
    public static final Capability SUBJECTS = Capability.named(NAMESPACE, "subjects");

    /** {@code assign entitlements { … }} — an allowance granted to one place. */
    public static final Capability ENTITLEMENTS = Capability.named(NAMESPACE, "entitlements");

    private static final Capability[] EVERY = {
            INCLUDE, SCOPES, PERMISSIONS, ACTIONS, VARIABLES,
            CAPABILITIES, ROLES, PLANS, SUBJECTS, ENTITLEMENTS
    };

    private PolicyCapability() {
    }

    /**
     * Every capability a policy file can ask for, in the order a document writes them.
     *
     * <p>Copied on the way out — a shared array is a set of constants anybody can quietly edit.</p>
     *
     * @return all ten
     */
    public static Capability[] every() {
        return EVERY.clone();
    }

    /**
     * Which capability a top-level declaration asks for.
     *
     * <p>⚠️ The list is deliberately the same one {@link org.jmouse.access.el.node.PolicyNode}
     * accepts. A node type it refuses to hold is a node type no destination can be asked to render,
     * so the two lists disagreeing would mean a document that parses and cannot be written back.</p>
     *
     * @param expression a child of a policy
     * @return the capability it needs, or {@code null} where a policy holds no such declaration
     */
    public static Capability of(Expression expression) {
        return switch (expression) {
            case IncludeNode ignored -> INCLUDE;
            case ScopesNode ignored -> SCOPES;
            case PermissionsNode ignored -> PERMISSIONS;
            case ActionsNode ignored -> ACTIONS;
            case VariablesNode ignored -> VARIABLES;
            case CapabilitiesNode ignored -> CAPABILITIES;
            case RoleNode ignored -> ROLES;
            case PlansNode ignored -> PLANS;
            case SubjectNode ignored -> SUBJECTS;
            case EntitlementsNode ignored -> ENTITLEMENTS;
            default -> null;
        };
    }

    /**
     * How a capability is written in a file, for a refusal somebody has to read.
     *
     * <p>{@code policy.entitlements} is the name; {@code entitlements} is the word in the file, and
     * the word in the file is what whoever is editing it recognises.</p>
     *
     * @param capability one of the constants above
     * @return its name without the namespace
     */
    public static String keyword(Capability capability) {
        String name = capability.name();

        return name.substring(name.indexOf('.') + 1);
    }
}
