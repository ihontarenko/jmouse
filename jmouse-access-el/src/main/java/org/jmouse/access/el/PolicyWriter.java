package org.jmouse.access.el;

import org.jmouse.access.el.node.GrantNode;
import org.jmouse.access.el.node.IncludeNode;
import org.jmouse.access.el.node.PermissionDeclarationNode;
import org.jmouse.access.el.node.PermissionsNode;
import org.jmouse.access.el.node.PolicyBlockNode;
import org.jmouse.access.el.node.PolicyNode;
import org.jmouse.access.el.node.RoleAssignmentNode;
import org.jmouse.access.el.node.RoleNode;
import org.jmouse.access.el.node.ScopeDeclarationNode;
import org.jmouse.access.el.node.ScopesNode;
import org.jmouse.access.el.node.SingleScopeNode;
import org.jmouse.access.el.node.SubjectNode;
import org.jmouse.access.policy.model.PolicyBundleEntry;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.access.policy.model.PolicyGrant;
import org.jmouse.access.policy.model.PolicyInclude;
import org.jmouse.access.policy.model.PolicyPermissionDeclaration;
import org.jmouse.access.policy.model.PolicyRole;
import org.jmouse.access.policy.model.PolicyRoleAssignment;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.access.policy.model.PolicyScopeDeclaration;
import org.jmouse.access.policy.model.PolicySubject;

/**
 * A {@link PolicyDocument} written as {@code .jmp} — including one that was never a file.
 *
 * <p>{@link ExpressionEvaluator#rewrite(String)} already renders a policy that was parsed. This
 * renders one that was <em>not</em>: a document projected out of a database, assembled by a form, or
 * built in a test. That is what lets a control room show a declarative expression for a rule nobody
 * ever declared, which is the whole reason the editor can exist.
 *
 * <h2>One renderer, not two that agree for a month</h2>
 *
 * <p>⚠️ The obvious implementation — walk the document, append strings — is a <strong>second
 * spelling of the grammar</strong>, and the first thing a second spelling gets wrong is quoting: a
 * workspace called {@code my-space} written bare comes back as {@code my} followed by a subtraction.
 * So this does not render anything. It builds the very nodes the parser builds and asks
 * <em>them</em> to render, which means the writer cannot drift from the parser because it is not a
 * separate implementation of anything. {@link SourceWriter} decides what is quoted, in one place, for
 * both directions.
 *
 * <p>The property that follows, and the one worth checking: parsing what this writes gives back the
 * same document, and writing that again changes not one character.
 *
 * <h2>What a rendering is not</h2>
 *
 * <p>⚠️ <strong>Comments do not survive.</strong> The parser does not keep them, so what comes out is
 * a rendering of the policy rather than an edit of somebody's text. A screen that saves over a file
 * has to say so before it does it the first time.
 *
 * <p>⚠️ <strong>Nor does the order inside a subject.</strong> A document holds a subject's role
 * assignments and its personal grants as two lists, so a file that interleaved them comes back with
 * the {@code grants} lines first. Nothing changes meaning — a policy is a set and the engine reads it
 * as one — but it is a diff, which is why {@link ExpressionEvaluator#rewrite(String)} exists beside
 * this: rewriting keeps a file's order because it renders the tree the parser built, and this one
 * renders a document that may never have had a tree.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PolicyWriter {

    private PolicyWriter() {
    }

    /**
     * Writes a document out as policy source.
     *
     * <p>A document that names itself is wrapped in {@code policy "…" { }}; one that does not is
     * written as its bare declarations, which is the form a file loaded under its own file name has.
     *
     * @param document the policy to write
     * @return its text, ready to be parsed back
     * @throws IllegalArgumentException where a name holds both kinds of quote and so has no spelling
     */
    public static String write(PolicyDocument document) {
        return toNode(document).toSource();
    }

    /**
     * Builds the node tree the parser would have built for this document.
     *
     * <p>The order is the language's own: what the file composes with, then the vocabulary it is
     * written against, then the roles, then who holds what. A reader meets a name after the block that
     * declares it.
     *
     * @param document the policy to build
     * @return the tree, ready to render
     */
    private static PolicyNode toNode(PolicyDocument document) {
        PolicyNode policy = new PolicyNode(document.name());

        for (PolicyInclude include : document.includes()) {
            policy.addExpression(toNode(include));
        }

        if (!document.scopes().isEmpty()) {
            policy.addExpression(toScopesNode(document));
        }

        if (!document.permissions().isEmpty()) {
            policy.addExpression(toPermissionsNode(document));
        }

        for (PolicyRole role : document.roles()) {
            policy.addExpression(toNode(role));
        }

        for (PolicySubject subject : document.subjects()) {
            policy.addExpression(toNode(subject));
        }

        return policy;
    }

    private static IncludeNode toNode(PolicyInclude include) {
        IncludeNode node = new IncludeNode();

        node.setPath(include.path());

        return node;
    }

    private static PolicyBlockNode toScopesNode(PolicyDocument document) {
        ScopesNode block = new ScopesNode();

        for (PolicyScopeDeclaration scope : document.scopes()) {
            block.addExpression(toNode(scope));
        }

        return block;
    }

    private static ScopeDeclarationNode toNode(PolicyScopeDeclaration scope) {
        ScopeDeclarationNode node = new ScopeDeclarationNode();

        node.setName(scope.name());
        node.setNature(scope.nature());
        node.setParameter(scope.parameter());

        return node;
    }

    private static PolicyBlockNode toPermissionsNode(PolicyDocument document) {
        PermissionsNode block = new PermissionsNode();

        for (PolicyPermissionDeclaration permission : document.permissions()) {
            block.addExpression(toNode(permission));
        }

        return block;
    }

    private static PermissionDeclarationNode toNode(PolicyPermissionDeclaration permission) {
        PermissionDeclarationNode node = new PermissionDeclarationNode();

        node.setName(permission.name());
        node.setDescription(permission.description());

        return node;
    }

    private static RoleNode toNode(PolicyRole role) {
        RoleNode node = new RoleNode(role.name());

        for (PolicyBundleEntry entry : role.bundle()) {
            node.addExpression(toNode(entry));
        }

        return node;
    }

    /**
     * One bundle entry.
     *
     * <p>Written with no instance, no effect and no condition — not because this leaves them out, but
     * because a bundle has none of the three. A role says what a permission is <em>worth</em>; which
     * instance, whether it is taken away and under what circumstances are all decisions about an
     * account, and {@link RoleNode} refuses to read them back.
     */
    private static GrantNode toNode(PolicyBundleEntry entry) {
        GrantNode node = new GrantNode();

        node.setScope(toNode(PolicyScope.kind(entry.scope())));
        node.setPermission(entry.permission());

        return node;
    }

    private static SubjectNode toNode(PolicySubject subject) {
        SubjectNode node = new SubjectNode(subject.id());

        for (PolicyRoleAssignment assignment : subject.roles()) {
            node.addExpression(toNode(assignment));
        }

        for (PolicyGrant grant : subject.grants()) {
            node.addExpression(toNode(grant));
        }

        return node;
    }

    private static RoleAssignmentNode toNode(PolicyRoleAssignment assignment) {
        RoleAssignmentNode node = new RoleAssignmentNode();

        node.setRoleName(assignment.roleName());
        node.setScope(toNode(assignment.scope()));

        return node;
    }

    /**
     * One personal grant.
     *
     * <p>⚠️ <strong>{@code allow} is left unwritten and {@code deny} never is.</strong> Nothing is
     * lost by that: the language implies {@code allow}, so a document cannot tell the terse form from
     * the spelled-out one — {@link PolicyGrant#effect()} is {@link PolicyEffect#ALLOW} either way, and
     * both parse back to the same record. What it buys is that the common line is written the common
     * way, and that a denial is the one thing on it a reader cannot skim past.
     */
    private static GrantNode toNode(PolicyGrant grant) {
        GrantNode node = new GrantNode();

        node.setScope(toNode(grant.scope()));
        node.setPermission(grant.permission());
        node.setCondition(grant.condition());

        if (grant.effect() == PolicyEffect.DENY) {
            node.setEffect(PolicyEffect.DENY);
        }

        return node;
    }

    private static SingleScopeNode toNode(PolicyScope scope) {
        SingleScopeNode node = new SingleScopeNode();

        node.setKind(scope.kind());
        node.setInstance(scope.instance());

        return node;
    }
}
