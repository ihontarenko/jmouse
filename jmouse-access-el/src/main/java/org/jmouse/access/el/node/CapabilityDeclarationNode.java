package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyCapabilityDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * One line of a {@code capabilities} block: {@code limit seat "Seats" per organization}.
 *
 * <p>The kind is a bare word the parser does not interpret — {@code gate}, {@code limit} or
 * {@code quota} — and binding maps it onto a kind the engine understands. Same arrangement as a scope
 * declaration's nature, and for the same reason: a parser that knew what {@code quota} meant would be
 * a parser holding half the model.</p>
 *
 * <p>⚠️ {@code paid} is not set here. It arrives from the {@code paid} lines of the same block, which
 * name keys and say nothing about shape — a capability can be paid <em>and</em> a limit, so the two
 * cannot be one field written in one place.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class CapabilityDeclarationNode extends AbstractExpression {

    private String       key;
    private String       kind;
    private String       displayName;
    private List<String> scopes = new ArrayList<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : scopes;
    }

    /**
     * Returns this declaration as the record stage 2 receives.
     *
     * @param paid whether one of the block's {@code paid} lines named this key
     */
    public PolicyCapabilityDeclaration toCapabilityDeclaration(boolean paid) {
        return new PolicyCapabilityDeclaration(
                getKey(), getDisplayName(), getKind(), getScopes(), paid, SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toCapabilityDeclaration(false);
    }

    @Override
    public String toSource() {
        StringBuilder source = new StringBuilder(getKind()).append(' ').append(SourceWriter.name(getKey()));

        if (displayName != null) {
            source.append(' ').append(SourceWriter.literal(getDisplayName()));
        }

        if (!scopes.isEmpty()) {
            source.append(" per ").append(String.join(", ", scopes));
        }

        return source.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
