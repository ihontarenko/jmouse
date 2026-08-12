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
 * <p>A role body holds bundle entries and nothing else. The two refusals below are one idea: a role
 * says what a permission is <em>worth</em> and how far it reaches — never <em>which</em> instance,
 * and never <em>less</em> than nothing.</p>
 *
 * <ul>
 *   <li>⚠️ <strong>No instance.</strong> {@code @SPACE space:write} means "as far as a workspace",
 *       and which workspace is decided where the role is assigned. Written with an instance the
 *       sentence has no meaning; written by a subject without one it grants in <em>every</em>
 *       workspace at once — the bug that once shipped in this product as a live escalation.</li>
 *   <li>⚠️ <strong>No {@code deny}.</strong> Deny wins globally and is applied last, so a denial in a
 *       bundle would take the permission from everybody holding the role, everywhere. Denials are
 *       per-subject.</li>
 * </ul>
 *
 * <h2>A condition is allowed, and the reason it used to be refused was obsolete</h2>
 *
 * <p>This block used to reject {@code when} on the grounds that "a bundle is resolved once, so a
 * predicate inside one makes that set a function of a row". That was true before {@code ConditionAxis}
 * and has not been true since: a condition is <strong>carried</strong> through resolution and read
 * afterwards, by an axis that sees an already-resolved target and may only narrow. The effective set
 * stays a function of {@code (subject, chain)}, the memoisation stays alive, and a listing filter
 * stays expressible — {@code DirectGrant} had been proving exactly that for months.
 *
 * <p>⚠️ <strong>What makes it safe is the refusal directly above it.</strong> With no way to write a
 * {@code deny} in a bundle, a {@code when} here can only ever subtract from an allow; there is no
 * syntax in this block that could express a conditional mass <em>denial</em>.
 *
 * <p>⚠️ <strong>What is left to be careful about is reach.</strong> A conditional allow that can never
 * hold takes the permission from <em>everybody holding the role</em> — so a name that does not resolve
 * stops being one person's missing permission and becomes an outage. That is why a property path is
 * refused at load rather than discovered on a Tuesday.
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
     * @return the entry stage 2 receives, condition and all
     * @throws PolicyParseException when the statement names an instance or an effect
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

        return new PolicyBundleEntry(
                grant.getPermission(), scope.getKind(), grant.getCondition(), SourceSpanNode.at(grant));
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
