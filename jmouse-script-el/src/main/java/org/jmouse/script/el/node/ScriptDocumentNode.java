package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.List;

/**
 * One parsed {@code .jms} file — <strong>the contract between the parser and everything after it</strong>.
 *
 * <h2>Everything in here is text</h2>
 *
 * <p>No events, no facades, no functions-as-types, no catalogue lookups: the parser reads characters
 * and reports faithfully what the file said. Whether {@code unload} is an event this host fires,
 * whether {@code @world} is a facade it declared and whether {@code yard_ready} was ever defined are
 * all questions for the binder, which has the catalogues.</p>
 *
 * <p>That separation is the point. A parser bug and a host bug then look nothing alike — one is a
 * syntax error with a position, the other is <em>"this host declares no event called unlod"</em>, and
 * neither can masquerade as the other.</p>
 *
 * <h2>⚠️ A tree, where {@code .jmp} produces records</h2>
 *
 * <p>{@code .jmp} parses to a node tree and evaluates it into a separate document of plain records,
 * because its model is consumed by modules that have no expression language on the classpath and its
 * conditions are re-compiled later against a narrower vocabulary.</p>
 *
 * <p>Neither holds here. A script's body <em>is</em> a compiled node tree — that is the whole
 * evaluation model — so a parallel record layer could only mirror the tree it was built from, and the
 * binder would have to re-parse the text it was handed in order to fill it. "Never a re-parse" is a
 * rule of this dialect, so the tree is the document and every <em>name</em> on it is a string.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptDocumentNode extends ExpressionsNode {

    private String name;

    /**
     * Constructs a document.
     *
     * @param name what the file calls itself, or {@code null} until a loader names it
     */
    public ScriptDocumentNode(String name) {
        this.name = name;
    }

    /**
     * Returns what this document is called.
     *
     * @return the name, or {@code null} while nothing has said
     */
    public String getName() {
        return name;
    }

    /**
     * Names a document that did not name itself — usually after the file it was read from.
     *
     * @param name what to call it
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the files this one composes with, in the order they were written.
     *
     * @return the include statements
     */
    public List<IncludeNode> getIncludes() {
        return getExpressions().stream().filter(IncludeNode.class::isInstance).map(IncludeNode.class::cast).toList();
    }

    /**
     * Returns the story halves — the {@code script "…" { … }} blocks.
     *
     * @return the script blocks
     */
    public List<ScriptNode> getScripts() {
        return getExpressions().stream().filter(ScriptNode.class::isInstance).map(ScriptNode.class::cast).toList();
    }

    /**
     * Returns the mechanic halves — the {@code behaviour "…" do … end} blocks.
     *
     * @return the behaviour blocks
     */
    public List<BehaviourNode> getBehaviours() {
        return getExpressions().stream()
                .filter(BehaviourNode.class::isInstance).map(BehaviourNode.class::cast).toList();
    }

    /**
     * ⚠️ Evaluating a document yields the document. It is a declaration, not a statement: what a host
     * runs is a handler or a function, reached through the bound script rather than by evaluating the
     * file.
     *
     * @param context the evaluation context, unused
     * @return this document
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        return this;
    }

    @Override
    public String toString() {
        return "SCRIPT_DOCUMENT['%s']".formatted(name == null ? "anonymous" : name);
    }

    /**
     * Adds a declaration to the file.
     *
     * @param declaration an include, a script block or a behaviour block
     */
    public void addDeclaration(Expression declaration) {
        addExpression(declaration);
    }

}
