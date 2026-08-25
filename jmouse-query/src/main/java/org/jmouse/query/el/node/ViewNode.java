package org.jmouse.query.el.node;

import org.jmouse.query.el.SourceWriter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code view "name" on target { … }} — a named, stored query.
 *
 * <p>⚠️ <strong>The target is an opaque identifier and is resolved by nobody here.</strong> Whether
 * {@code on inventory} names a section, a purpose, or both is a question each product answers about
 * its own data, and a language that answered it would be a language that could only serve one product.
 * The parser reads the word and hands it on.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ViewNode extends QueryBlockNode {

    private static final String INDENT = "  ";

    private final List<ParameterDeclarationNode> parameters = new ArrayList<>();
    private final List<ParameterDeclarationNode> ambient    = new ArrayList<>();

    private String  title;
    private String  identifier;
    private String  target;
    private boolean targetBound;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * What this view is <strong>referenced by</strong> — {@code view 'Гарячі':hot}.
     *
     * <h2>⚠️ Not the same thing as the title, and separating them is what makes a view an object</h2>
     *
     * <p>A title is shown on a screen, translated, and changed by whoever owns the screen. An identifier
     * is what another declaration writes down. While a view had only a title, a second view naming it
     * would break the moment somebody improved the wording — so a view could not be referenced at all,
     * which is why a subquery had nowhere to live.</p>
     */
    public Optional<String> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * What a caller must supply for this view to mean anything — {@code hot(since as temporal : now())}.
     *
     * <p>⚠️ A type here is not validation. It is what lets a compiler <strong>bind</strong> rather than
     * substitute: a collection parameter becomes one placeholder per element, and nothing is ever
     * concatenated into query text.</p>
     */
    public List<ParameterDeclarationNode> getParameters() {
        return List.copyOf(parameters);
    }

    public void addParameter(ParameterDeclarationNode parameter) {
        parameters.add(parameter);
    }

    /**
     * The ambient values this view is allowed to read — {@code uses(prefix as text)}.
     *
     * <h2>⚠️ Declared, because the alternative reads somebody else's data</h2>
     *
     * <p>A value set on the evaluation context somewhere above — a tenant, a prefix, the current
     * member — is not a parameter: nobody passes it at the call. Left undeclared, a view reading one runs
     * only where somebody remembered to set it, and where they did not it is either null or
     * <strong>another person's value</strong>, with no message anywhere.</p>
     *
     * <p>So a view says which it reads, and a name it never declared is refused rather than read through.
     * ⚠️ What must never happen is "null when missing".</p>
     */
    public List<ParameterDeclarationNode> getAmbient() {
        return List.copyOf(ambient);
    }

    public void addAmbient(ParameterDeclarationNode value) {
        ambient.add(value);
    }

    /** Every name this view may legally mention beyond its own attributes. */
    public Set<String> declaredNames() {
        Set<String> names = new LinkedHashSet<>();

        parameters.forEach(parameter -> names.add(parameter.getName()));
        ambient.forEach(value -> names.add(value.getName()));

        return names;
    }

    /**
     * What this view is about — the name written after {@code on}.
     *
     * <p>⚠️ When {@link #isTargetBound()} is set this is the name of a <strong>binding</strong> rather
     * than of a source, and resolving it is the runtime's business. It is deliberately still just a name
     * here: a document that could name a table would be a document able to reach data nobody granted it.</p>
     */
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Whether the document wrote {@code on $something} rather than {@code on something}.
     *
     * <h2>⚠️ Late binding is what makes one view portable across backends</h2>
     *
     * <p>{@code on issues} pins the view to a source declared beside it. {@code on $source} says the view
     * does not know what it will run against until it is run — which is precisely the seam that lets one
     * text be checked against a list of maps in a test and compiled against a database in production,
     * without a second copy of the query existing anywhere.</p>
     *
     * <p>⚠️ And it resolves to a source the application <strong>declared</strong>, never to whatever
     * string the caller happened to bind. The binding chooses among sources; it does not create one.</p>
     */
    public boolean isTargetBound() {
        return targetBound;
    }

    public void setTargetBound(boolean targetBound) {
        this.targetBound = targetBound;
    }

    /**
     * ⚠️ {@code from:} is written as the first line of the BODY, never in the header.
     *
     * <p>What a block is about is a thing the block says about itself, like every other line in it. Held
     * in the header it was the one statement written in a different place and a different shape from the
     * rest, which is exactly the sort of exception somebody has to remember rather than read.</p>
     *
     * <p>The header form is still <em>read</em>, so nothing stored stops working — and never written.</p>
     */
    @Override
    public String toSource() {
        String header = headerToSource();
        String body   = bodyToSource();

        return body.isEmpty()
                ? "%s { }".formatted(header)
                : "%s {\n%s\n}".formatted(header, body);
    }

    private String bodyToSource() {
        String clauses = clausesToSource(INDENT);
        String subject = target == null ? "" : INDENT + "from: " + targetToSource();

        if (subject.isEmpty()) {
            return clauses;
        }

        return clauses.isEmpty() ? subject : subject + "\n" + clauses;
    }

    /**
     * ⚠️ An unnamed view writes {@code view {}, not {@code view null {}.
     *
     * <p>A view assembled in code — a filter and a sort handed to a compiler, with nobody to name it —
     * has no title, and {@code literal(null)} rendered the four letters of the word. It looked like a
     * view somebody had named <em>null</em>, which is worse than looking unnamed: it reads as data.</p>
     */
    private String headerToSource() {
        StringBuilder written = new StringBuilder("view");

        if (title != null) {
            written.append(' ').append(SourceWriter.literal(title));
        }

        if (identifier != null) {
            written.append(':').append(SourceWriter.name(identifier));
        }

        if (!parameters.isEmpty()) {
            written.append('(').append(declarationsToSource(parameters)).append(')');
        }

        if (!ambient.isEmpty()) {
            written.append(" uses(").append(declarationsToSource(ambient)).append(')');
        }

        return written.toString();
    }

    private String declarationsToSource(List<ParameterDeclarationNode> declared) {
        return declared.stream().map(ParameterDeclarationNode::toSource).collect(Collectors.joining(", "));
    }

    /** ⚠️ The {@code $} survives the round trip, or a late-bound view would come back pinned. */
    private String targetToSource() {
        return targetBound ? "$" + target : SourceWriter.name(target);
    }

    @Override
    public String toString() {
        return "view '%s' on %s".formatted(title, targetToSource());
    }
}
