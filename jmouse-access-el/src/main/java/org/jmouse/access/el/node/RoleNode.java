package org.jmouse.access.el.node;

import org.jmouse.access.el.PolicyParseException;
import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyBundleEntry;
import org.jmouse.access.policy.model.PolicyRole;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code role SPACE_ADMIN { … }} — a bundle of permissions and how far each one reaches.
 *
 * <p>A role body holds bundle entries and nothing else. The three refusals below are all one idea:
 * a role says what a permission is <em>worth</em>, never who has it or when.</p>
 *
 * <ul>
 *   <li>⚠️ <strong>No instance.</strong> {@code @SPACE space:write} means "as far as a workspace",
 *       and which workspace is decided where the role is assigned. Written with an instance the
 *       sentence has no meaning; written by a subject without one it grants in <em>every</em>
 *       workspace at once — the bug that once shipped in this product as a live escalation.</li>
 *   <li>⚠️ <strong>No {@code deny}.</strong> Deny wins globally and is applied last, so a denial in a
 *       bundle would take the permission from everybody holding the role, everywhere. Denials are
 *       per-subject.</li>
 *   <li>⚠️ <strong>No conditions.</strong> A bundle is resolved once into a set; a predicate inside
 *       one makes that set a function of a row.</li>
 * </ul>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RoleNode extends PolicyBlockNode {

    private final String name;

    public RoleNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns this role as the record stage 2 receives.
     *
     * @return the name, the bundle, and where it was written
     * @throws PolicyParseException when the body holds anything a bundle cannot carry
     */
    public PolicyRole toRole() {
        List<PolicyBundleEntry> bundle = new ArrayList<>();

        for (Expression expression : getExpressions()) {
            if (expression instanceof GrantNode grant) {
                bundle.add(toBundleEntry(grant));
            } else {
                throw reject(expression, "a role body holds only entries like '@SPACE space:write'");
            }
        }

        return new PolicyRole(getName(), bundle, SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toRole();
    }

    /**
     * Reads one statement as a bundle entry, refusing everything a bundle cannot carry.
     *
     * @param grant the statement as parsed
     * @return the entry stage 2 receives
     * @throws PolicyParseException when the statement names an instance, an effect or a condition
     */
    private PolicyBundleEntry toBundleEntry(GrantNode grant) {
        SingleScopeNode scope = grant.getScope();

        if (scope.namesAnInstance()) {
            throw reject(grant, ("a role reaches a scope, it does not name one: write '@%s %s' and choose "
                    + "the instance where the role is assigned").formatted(scope.getKind(), grant.getPermission()));
        }

        if (grant.getEffect() != null) {
            throw reject(grant, "a role grants and never takes away, so it carries no 'allow' or 'deny'");
        }

        if (grant.isConditional()) {
            throw reject(grant, "a bundle is resolved once, so it carries no 'when'; conditions are per-subject");
        }

        return new PolicyBundleEntry(grant.getPermission(), scope.getKind(), SourceSpanNode.at(grant));
    }

    @Override
    protected String describe() {
        return "a role body";
    }

    @Override
    public String toSource() {
        return renderBlock("role " + SourceWriter.name(getName()));
    }

    @Override
    public String toString() {
        return "role " + getName();
    }
}
