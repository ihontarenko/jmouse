package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.*;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code policy "name" { … }} — one parsed file, and everything it declared.
 *
 * <p>The wrapper is optional; a file without it is still valid and the document name then falls back
 * to the file name. The name is carried into every grant's provenance, so the control room can say
 * <em>"granted by policy innoventa-bootstrap"</em> rather than showing an origin of nothing.</p>
 *
 * <p>What comes out is {@link PolicyDocument} — plain strings, no engine types, nothing that knows a
 * catalogue exists. Whether {@code SPCE} is a scope, whether {@code form:read} was ever registered,
 * whether a condition compiles: all stage 2, all so that a parser bug and a policy bug never look
 * alike to whoever reads the message.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PolicyNode extends PolicyBlockNode {

    private final String name;

    /**
     * Constructs a policy.
     *
     * @param name the name from the {@code policy "…"} header, or {@code null} for a file that
     *             declared its roles and subjects without a wrapper — the loader names that one after
     *             the file it read
     */
    public PolicyNode(String name) {
        this.name = name;
    }

    /**
     * Returns the name written in the header.
     *
     * @return the name, or {@code null} where the file carried no wrapper
     */
    public String getName() {
        return name;
    }

    /**
     * Whether this file named itself.
     *
     * @return {@code true} when a {@code policy "…"} header was written
     */
    public boolean isNamed() {
        return name != null && !name.isBlank();
    }

    /**
     * Returns this file as the document stage 2 receives.
     *
     * @return everything the file declared, as plain strings
     * @throws org.jmouse.access.el.PolicyParseException when a statement was written where a policy
     *                                                   cannot hold it
     */
    public PolicyDocument toDocument() {
        List<PolicyInclude>               includes     = new ArrayList<>();
        List<PolicyScopeDeclaration>      scopes       = new ArrayList<>();
        List<PolicyPermissionDeclaration> permissions  = new ArrayList<>();
        List<PolicyActionDeclaration>     actions      = new ArrayList<>();
        List<PolicyCapabilityDeclaration> capabilities = new ArrayList<>();
        List<PolicyRole>                  roles        = new ArrayList<>();
        List<PolicyPlan>                  plans        = new ArrayList<>();
        List<PolicySubject>               subjects     = new ArrayList<>();
        List<PolicyEntitlement>           entitlements = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof IncludeNode include) {
                includes.add(include.toInclude());
            } else if (expression instanceof ScopesNode block) {
                scopes.addAll(block.toScopeDeclarations());
            } else if (expression instanceof PermissionsNode block) {
                permissions.addAll(block.toPermissionDeclarations());
            } else if (expression instanceof ActionsNode block) {
                actions.addAll(block.toActionDeclarations());
            } else if (expression instanceof CapabilitiesNode block) {
                capabilities.addAll(block.toCapabilityDeclarations());
            } else if (expression instanceof RoleNode role) {
                roles.add(role.toRole());
            } else if (expression instanceof PlansNode block) {
                plans.addAll(block.toPlans());
            } else if (expression instanceof SubjectNode subject) {
                subjects.add(subject.toSubject());
            } else if (expression instanceof EntitlementsNode block) {
                entitlements.addAll(block.toEntitlements());
            } else {
                throw reject(expression, "a policy holds 'include', 'scopes', 'permissions', "
                        + "'actions', 'capabilities', 'role', 'plans', 'subject' and 'entitlements' "
                        + "declarations");
            }
        }

        return new PolicyDocument(getName(), includes, scopes, permissions, actions, capabilities,
                                  roles, plans, subjects, entitlements);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toDocument();
    }

    @Override
    protected String describe() {
        return "a policy";
    }

    @Override
    public String toSource() {
        if (!isNamed()) {
            return renderExpressions(0);
        }

        return renderBlock("policy " + SourceWriter.literal(getName()));
    }

    @Override
    public String toString() {
        return isNamed() ? "policy " + getName() : "policy";
    }
}
