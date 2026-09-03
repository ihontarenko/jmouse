package org.jmouse.script.el.host;

import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.language.node.IfBranchNode;
import org.jmouse.el.language.node.IfNode;
import org.jmouse.script.el.ScriptEvaluator;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.SourceSpan;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.node.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The seam between jMS and whatever embeds it.
 *
 * <p>This is what makes the dialect universal rather than a feature of one product. A host builds one of
 * these, declaring what it is willing to expose, and gets back the two things a library can honestly
 * offer: something that turns text into a runnable document, and something that runs it.</p>
 *
 * <h2>What a host declares</h2>
 *
 * <ul>
 *   <li>a {@link ScriptCatalogue} — the facades an {@code @} call may reach, the events a handler may be
 *       written against, and any functions of its own;</li>
 *   <li>{@link ScriptResources} — how an {@code include} finds a file, because only the host knows what
 *       a path means in its own storage;</li>
 *   <li>a ceiling ({@link ScriptBudget}) — the most any script loaded here may ask for.</li>
 * </ul>
 *
 * <h2>What it gives back</h2>
 *
 * <ul>
 *   <li>{@link #load} — text to a {@link BoundScript}: parsed, its includes followed, every name checked
 *       against the catalogue, the whole thing refused if any of it is wrong.</li>
 *   <li>{@link #newContext} — a context wired to the catalogue and to nothing else.</li>
 *   <li>{@link #dispatch} — an event name and a context, in; the matching handlers, run.</li>
 *   <li>{@link #call} — a declared function, by name, on demand.</li>
 * </ul>
 *
 * <h2>⚠️ Three rules this class is held to</h2>
 *
 * <p><strong>No consuming product appears in this module.</strong> Not on the classpath, not in a type
 * name, not in a javadoc example. A facade called {@code world} is a test fixture's name, never the
 * library's.</p>
 *
 * <p><strong>Dispatch does not decide <em>when</em> to fire.</strong> A host owns its clock, its event
 * bus and its threading; this owns compilation and evaluation and nothing else. There is no scheduler
 * here and there is not going to be one — a library that decided when a game's tick happened would be a
 * library the game had to work around.</p>
 *
 * <p><strong>Failures at load carry a position; failures at evaluation carry the handler and the
 * event.</strong> They are different questions asked by different people, and a single exception type
 * for both would answer neither.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptHost {

    private final ScriptCatalogue catalogue;
    private final ScriptResources resources;
    private final ScriptEvaluator evaluator;
    private final ScriptBinder    binder;

    private ScriptHost(ScriptCatalogue catalogue, ScriptResources resources, ScriptBudget ceiling) {
        this.catalogue = catalogue;
        this.resources = resources;
        this.evaluator = new ScriptEvaluator();
        this.binder = new ScriptBinder(catalogue, ceiling);
    }

    /**
     * Starts describing a host.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns what this host declared.
     *
     * @return the catalogue
     */
    public ScriptCatalogue getCatalogue() {
        return catalogue;
    }

    /**
     * Loads a script: parses it, follows its includes, and binds the result against the catalogue.
     *
     * @param name   what to call it — a file name, a row id, whatever a person could open
     * @param source the text
     * @return the bound script
     * @throws ScriptParseException when the text is not a script
     * @throws ScriptBindException  when it names something this host did not declare
     */
    public BoundScript load(String name, String source) {
        return load(name, source, ScriptBudget.unlimited());
    }

    /**
     * Loads a script under a budget it asked for, clamped to this host's ceiling.
     *
     * @param name      what to call it
     * @param source    the text
     * @param requested what the document asked for — from wherever the host stores it
     * @return the bound script
     */
    public BoundScript load(String name, String source, ScriptBudget requested) {
        ScriptDocumentNode merged = new ScriptDocumentNode(name);

        gather(name, source, merged, new LinkedHashSet<>(), new ArrayDeque<>());

        return binder.bind(merged, requested);
    }

    /**
     * Builds an evaluation context wired to this host's catalogue, and to nothing else.
     *
     * <p>⚠️ {@link FacadeLookup} is the line that matters: it is what makes {@code @name} mean a declared
     * facade rather than "whatever bean the application has under that name". A host assembling its own
     * context must install it too, or the closed catalogue is decorative.</p>
     *
     * <p>A context is reusable across events — building one per event is what
     * {@link BoundScript#installFunctions} exists to avoid — so a host normally keeps one per script and
     * per thread.</p>
     *
     * @param bound the script that will run in it
     * @return a context ready to dispatch through
     */
    public EvaluationContext newContext(BoundScript bound) {
        DefaultEvaluationContext context = new DefaultEvaluationContext();

        context.setExtensions(evaluator.getExtensions());
        context.setBeanLookup(new FacadeLookup(catalogue));
        bound.installFunctions(context);

        return context;
    }

    /**
     * Fires one event: runs every handler written for it, in the order they were written.
     *
     * <p>⚠️ <strong>One budget for the whole dispatch</strong>, not one per handler — three handlers for
     * one event are one thing the host asked for.</p>
     *
     * @param bound   the script
     * @param event   the event name
     * @param context the context, carrying whatever this event hands a handler
     * @return how many handlers actually ran — a guard may refuse some of them
     * @throws ScriptDispatchException when a handler fails while running
     */
    public int dispatch(BoundScript bound, String event, EvaluationContext context) {
        List<HandlerNode> handlers = bound.handlersFor(event);

        if (handlers.isEmpty()) {
            return 0;
        }

        int ran = 0;

        bound.begin(context);

        try {
            for (HandlerNode handler : handlers) {
                if (handler.matches(context)) {
                    run(bound, event, handler, context);
                    ran++;
                }
            }
        } finally {
            bound.finish(context);
        }

        return ran;
    }

    /**
     * Calls a function the script declared.
     *
     * @param bound     the script
     * @param function  the function's name
     * @param context   the evaluation context
     * @param arguments the arguments, in declaration order
     * @return whatever the function returned
     * @throws ScriptDispatchException when it fails while running
     */
    public Object call(BoundScript bound, String function, EvaluationContext context, Object... arguments) {
        bound.begin(context);

        try {
            return bound.invoke(function, context, arguments);
        } catch (ScriptDispatchException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new ScriptDispatchException(bound.getName(), function, "a call", failure);
        } finally {
            bound.finish(context);
        }
    }

    /**
     * ⚠️ The guard runs outside this, on purpose. A {@code when} clause that throws is a failure of the
     * <em>guard</em>, and reporting it as "the handler failed" sends a reader to the wrong half of the
     * line — but a guard that merely answers false is not a failure at all, and must not look like one.
     */
    private void run(BoundScript bound, String event, HandlerNode handler, EvaluationContext context) {
        try {
            handler.evaluate(context);
        } catch (ScriptDispatchException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new ScriptDispatchException(bound.getName(), event, "a handler", failure);
        }
    }

    /**
     * Reads one file, follows what it includes, and adds everything to one document.
     *
     * <p>⚠️ <strong>Includes are followed depth-first and before the includer's own declarations</strong>,
     * so a function a file relies on is already there when the file that uses it is read. It also makes
     * the duplicate-name refusal point at the <em>second</em> declaration, which is the one somebody
     * added.</p>
     *
     * <p>⚠️ <strong>Every span is stamped with the file it came from, here.</strong> After the merge
     * "line 12" belongs to nothing in particular, and a refusal that cannot say which of four files it
     * means is a refusal nobody can act on. This is the only place that knows both.</p>
     *
     * @param name    what this file is called
     * @param source  its text
     * @param merged  the document being assembled
     * @param seen    every file already merged, so one included twice is read once
     * @param loading the files currently open, so a cycle is refused rather than followed
     */
    private void gather(
            String name,
            String source,
            ScriptDocumentNode merged,
            Set<String> seen,
            Deque<String> loading
    ) {
        if (!seen.add(name)) {
            return;
        }

        loading.push(name);

        ScriptDocumentNode parsed = evaluator.parse(source, name);

        parsed.getExpressions().forEach(declaration -> stamp(declaration, name));

        for (IncludeNode include : parsed.getIncludes()) {
            follow(include, merged, seen, loading);
        }

        for (Expression declaration : parsed.getExpressions()) {
            if (!(declaration instanceof IncludeNode)) {
                merged.addDeclaration(declaration);
            }
        }

        loading.pop();
    }

    private void follow(
            IncludeNode include, ScriptDocumentNode merged, Set<String> seen, Deque<String> loading) {

        String path = include.getPath();

        if (loading.contains(path)) {
            throw new ScriptParseException(
                    ScriptSpanNode.at(include),
                    "'%s' includes itself, through %s".formatted(path, String.join(" -> ", loading)));
        }

        String included = resources.read(path);

        if (included == null) {
            throw new ScriptParseException(
                    ScriptSpanNode.at(include),
                    "there is no script called '%s' to include".formatted(path));
        }

        gather(path, included, merged, seen, loading);
    }

    /**
     * Names the file on a node's span, and on every span below it.
     *
     * <p>Typed rather than reflective, for the reason the binder's own walk is: a shape this does not
     * know is a shape whose positions would silently lose their file, and a position that names the
     * wrong file is worse than one that names none.</p>
     */
    private void stamp(Expression expression, String document) {
        if (expression instanceof AbstractExpression node && node.getSpan() instanceof ScriptSpanNode span) {
            span.setDocument(document);
        }

        switch (expression) {
            case ScriptNode script -> script.getExpressions().forEach(child -> stamp(child, document));
            case BehaviourNode behaviour -> behaviour.getExpressions().forEach(child -> stamp(child, document));
            case ScriptBodyNode body -> body.getExpressions().forEach(child -> stamp(child, document));
            case IfNode branch -> {
                for (IfBranchNode alternative : branch.getBranches()) {
                    alternative.getExpressions().forEach(child -> stamp(child, document));
                }
            }
            default -> {
                // A leaf — a local, an assignment, a return, a bare call. Its own span is already named.
            }
        }
    }

    /**
     * Describes a host: what it exposes, where its files come from, and how much they may do.
     */
    public static final class Builder {

        private ScriptCatalogue catalogue = ScriptCatalogue.empty();
        private ScriptResources resources = ScriptResources.none();
        private ScriptBudget    ceiling   = ScriptBudget.unlimited();

        private Builder() {
        }

        /**
         * Declares what scripts may reach.
         *
         * @param catalogue the facades, events and functions
         * @return this builder
         */
        public Builder catalogue(ScriptCatalogue catalogue) {
            this.catalogue = catalogue == null ? ScriptCatalogue.empty() : catalogue;
            return this;
        }

        /**
         * Declares where an {@code include} finds a file.
         *
         * @param resources the resource loader
         * @return this builder
         */
        public Builder resources(ScriptResources resources) {
            this.resources = resources == null ? ScriptResources.none() : resources;
            return this;
        }

        /**
         * Declares the most any script loaded here may ask for.
         *
         * <p>⚠️ Leave it unset only where the same people write the host and the scripts. A host whose
         * users author scripts and declares no ceiling has no guard at all — see {@link ScriptBudget}.</p>
         *
         * @param ceiling the ceiling
         * @return this builder
         */
        public Builder ceiling(ScriptBudget ceiling) {
            this.ceiling = ceiling == null ? ScriptBudget.unlimited() : ceiling;
            return this;
        }

        /**
         * Builds the host.
         *
         * @return a host ready to load scripts
         */
        public ScriptHost build() {
            return new ScriptHost(catalogue, resources, ceiling);
        }
    }
}
