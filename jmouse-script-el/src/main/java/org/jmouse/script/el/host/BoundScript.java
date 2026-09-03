package org.jmouse.script.el.host;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Lambda;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.budget.ScriptExecution;
import org.jmouse.script.el.node.BehaviourNode;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.node.HandlerNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A document that has met a host: every name in it exists, and everything is indexed by what a host
 * asks for at run time.
 *
 * <h2>⚠️ Built once, at load</h2>
 *
 * <p>Everything a host will evaluate later is produced here and never rebuilt. Dispatching an event is
 * a map lookup and a walk of a node tree that already exists — no parse, no reflection, no name
 * resolution. That is the whole reason the binder is a separate stage rather than something that
 * happens on the first event.</p>
 *
 * <p>This object is immutable and holds the parser's nodes rather than copies of them, so binding a
 * document twice produces two instances that are {@link #equals equal} — which is the observable form
 * of "the binder does not modify what it was given".</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class BoundScript {

    private final String                                 name;
    private final Map<String, List<HandlerNode>>         handlers;
    private final Map<String, FunctionDeclarationNode>   functions;
    private final Map<String, BehaviourNode>             behaviours;
    private final List<String>                           includes;
    private final Map<String, Lambda>                    callables;
    private final ScriptBudget                           budget;

    BoundScript(
            String name,
            Map<String, List<HandlerNode>> handlers,
            Map<String, FunctionDeclarationNode> functions,
            Map<String, BehaviourNode> behaviours,
            List<String> includes,
            ScriptBudget budget
    ) {
        this.name = name;
        this.handlers = Map.copyOf(handlers);
        this.functions = Map.copyOf(functions);
        this.behaviours = Map.copyOf(behaviours);
        this.includes = List.copyOf(includes);
        this.callables = callables(this.functions);
        this.budget = budget == null ? ScriptBudget.unlimited() : budget;
    }

    /**
     * Returns how much one dispatch of this script may do — already clamped to the host's ceiling.
     *
     * <p>⚠️ This is the <em>effective</em> budget, not what the document asked for. The request belongs
     * to whoever stored it, and they can show an author the difference; a second copy here would be a
     * second copy to keep true.</p>
     *
     * @return the effective budget
     */
    public ScriptBudget getBudget() {
        return budget;
    }

    /**
     * Starts one dispatch: installs a fresh allowance into the context.
     *
     * <p>A host calls this immediately before evaluating a handler or a function, and {@link #finish}
     * when it is done. ⚠️ <strong>Per dispatch, never per script</strong> — two events are two budgets,
     * and a handler that spent its whole allowance an hour ago starts this one with all of it.</p>
     *
     * <p>⚠️ A script whose effective budget stops nothing installs nothing, so a host that declared no
     * ceiling and stores no request pays for none of this.</p>
     *
     * @param context the evaluation context this dispatch will run in
     */
    public void begin(EvaluationContext context) {
        ScriptExecution execution = ScriptExecution.begin(name, budget);

        if (execution != null) {
            execution.installInto(context);
        }
    }

    /**
     * Ends one dispatch.
     *
     * <p>⚠️ Belongs in a {@code finally}. A context reused across events — the ordinary case, because
     * building one per event is what {@link #installFunctions} exists to avoid — would otherwise carry a
     * spent allowance into the next dispatch and refuse it for what its predecessor did.</p>
     *
     * @param context the evaluation context
     */
    public void finish(EvaluationContext context) {
        ScriptExecution.clear(context);
    }

    /**
     * Wraps each declared function once, at load, as something an expression can call.
     *
     * <p>{@link org.jmouse.el.node.expression.FunctionNode} resolves a name it does not find among the
     * registered functions by looking for a {@link Lambda} in the evaluation context — which is exactly
     * what a script-declared function is: named parameters, a body, and a scope of its own while it
     * runs. So {@code busy()} in a guard reaches {@code function busy()} in the file without a second
     * dispatch mechanism, and without registering anything globally where two scripts would collide.</p>
     *
     * <p>⚠️ Built here rather than per call. The whole promise of this stage is that nothing is
     * constructed on a path a host may take sixty times a second.</p>
     */
    private static Map<String, Lambda> callables(Map<String, FunctionDeclarationNode> functions) {
        Map<String, Lambda> callables = new LinkedHashMap<>();

        functions.forEach((name, declaration) -> {
            Lambda callable = new Lambda(declaration);

            callable.setName(name);
            declaration.getParameters().forEach(callable::addParameter);

            callables.put(name, callable);
        });

        return Map.copyOf(callables);
    }

    /**
     * Makes this script's own functions callable from its expressions, in one context.
     *
     * <p>A host calls this once per context it will dispatch through — never per event.</p>
     *
     * <p>⚠️ <strong>Per context, not globally.</strong> Two scripts loaded in one process routinely
     * declare a function of the same name, and they are not the same function; a registry shared by the
     * whole expression language would let the second quietly answer for the first.</p>
     *
     * @param context the evaluation context a host will dispatch through
     */
    public void installFunctions(EvaluationContext context) {
        callables.forEach(context::setValue);
    }

    /**
     * Calls a function the script declared, binding the arguments to its parameters.
     *
     * <p>⚠️ <strong>Through the same {@link Lambda} an expression inside the script would reach</strong>,
     * rather than by evaluating the declaration directly. The Lambda is what opens a scope and binds the
     * parameters into it; calling the node itself would run the body against whatever happened to be in
     * the context, which reads as working right up until two parameters have the same names as two
     * things the host put there.</p>
     *
     * @param function  the function's name
     * @param context   the evaluation context
     * @param arguments the arguments, in declaration order
     * @return whatever the function returned
     * @throws IllegalArgumentException when the script declares no such function
     */
    public Object invoke(String function, EvaluationContext context, Object... arguments) {
        Lambda callable = callables.get(function);

        if (callable == null) {
            throw new IllegalArgumentException(
                    "'%s' declares no function called '%s'; it declares %s"
                            .formatted(name, function, callables.keySet()));
        }

        return callable.execute(Arguments.forArray(arguments), context);
    }

    /**
     * Returns what the document was called.
     *
     * @return the document name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the handlers written for an event, in the order they were written.
     *
     * <p>An event nobody wrote a handler for answers an empty list rather than {@code null}: a host
     * dispatching every event it fires should not have to ask whether anybody was listening.</p>
     *
     * @param event the event name
     * @return the handlers, possibly empty
     */
    public List<HandlerNode> handlersFor(String event) {
        return handlers.getOrDefault(event, List.of());
    }

    /**
     * Returns the events this script actually listens to.
     *
     * <p>Useful to a host that would rather not build a context for an event nobody handles.</p>
     *
     * @return the handled event names
     */
    public Set<String> handledEvents() {
        return handlers.keySet();
    }

    /**
     * Returns a function the script declared.
     *
     * @param name the function name
     * @return the declaration, or {@code null} when the script declares no such function
     */
    public FunctionDeclarationNode function(String name) {
        return functions.get(name);
    }

    /**
     * Returns a behaviour the script declared.
     *
     * @param name the behaviour name
     * @return the declaration, or {@code null} when the script declares no such behaviour
     */
    public BehaviourNode behaviour(String name) {
        return behaviours.get(name);
    }

    /**
     * Returns the files this document said it composes with, in the order they were written.
     *
     * <p>⚠️ Recorded, not followed. Resolving a path and detecting a cycle across a whole load is a
     * loader's work, and a binder that chased an include would be a binder that reads the file system.</p>
     *
     * @return the include paths
     */
    public List<String> getIncludes() {
        return includes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof BoundScript bound
                && Objects.equals(name, bound.name)
                && handlers.equals(bound.handlers)
                && functions.equals(bound.functions)
                && behaviours.equals(bound.behaviours)
                && includes.equals(bound.includes)
                && budget.equals(bound.budget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, handlers, functions, behaviours, includes, budget);
    }

    @Override
    public String toString() {
        return "BOUND_SCRIPT['%s': %d events, %d functions, %d behaviours]"
                .formatted(name, handlers.size(), functions.size(), behaviours.size());
    }
}
