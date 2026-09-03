package org.jmouse.script.el.host;

import org.jmouse.el.language.node.IfBranchNode;
import org.jmouse.el.language.node.IfNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.BeanAccessNode;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.script.el.ScriptExtension;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.SourceSpan;
import org.jmouse.script.el.node.*;

import java.util.*;

/**
 * Turns a parsed document into something a host can run — once, at load.
 *
 * <h2>What it binds against</h2>
 *
 * <p>The catalogue is supplied by the host: the events a handler may declare, the facades an {@code @}
 * call may resolve, the functions an expression may name. The library ships no catalogue of its own and
 * a host with an empty one is legal — it gets a document that binds to nothing, which is a better
 * answer than a document that binds to everything.</p>
 *
 * <h2>⚠️ Failure is a load-time error</h2>
 *
 * <p>A name absent from the catalogue means <strong>refuse to load</strong>, with the file, the line and
 * the offending name. Never a silent no-op, and never a failure deferred to the first time a handler
 * happens to fire — a script that stops working on minute forty of a session, with no message anywhere,
 * is the failure mode this whole stage exists to prevent.</p>
 *
 * <h2>⚠️ It reads the document and does not touch it</h2>
 *
 * <p>Nothing here mutates a node. Binding the same document twice produces two
 * {@link BoundScript#equals equal} results, which is the observable form of that promise and the thing
 * a test can actually assert.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptBinder {

    private final ScriptCatalogue catalogue;
    private final ScriptBudget    ceiling;
    private final Set<String>     filters;
    private final Set<String>     tests;

    /**
     * Constructs a binder for a host that bounds nothing.
     *
     * <p>Correct for a host that writes its own scripts and owns its own loop — a game. ⚠️ Not correct
     * for a host whose users author scripts; see the other constructor.</p>
     *
     * @param catalogue what that host declares
     */
    public ScriptBinder(ScriptCatalogue catalogue) {
        this(catalogue, ScriptBudget.unlimited());
    }

    /**
     * Constructs a binder for one host, with the most any script here may ever ask for.
     *
     * <p>⚠️ <strong>The ceiling belongs to the host and must live somewhere its authors cannot
     * reach.</strong> A budget stored beside the source and edited on the same form is a budget the
     * author of a runaway loop sets for themselves — see {@link ScriptBudget}. Where a ceiling comes
     * from — a constant, a property, a plan entitlement — is the host's business; this only has to be
     * handed a number.</p>
     *
     * @param catalogue what that host declares
     * @param ceiling   the most any document bound here may ask for
     */
    public ScriptBinder(ScriptCatalogue catalogue, ScriptBudget ceiling) {
        this.catalogue = catalogue;
        this.ceiling = ceiling == null ? ScriptBudget.unlimited() : ceiling;
        this.filters = ScriptExtension.builtinFilters();
        this.tests = ScriptExtension.builtinTests();
    }

    /**
     * Binds a document that asks for no budget of its own, so it runs at the host's ceiling.
     *
     * @param document the parsed document
     * @return the bound script, indexed by what a host asks for
     * @throws ScriptBindException when the document names something the host did not declare
     */
    public BoundScript bind(ScriptDocumentNode document) {
        return bind(document, ScriptBudget.unlimited());
    }

    /**
     * Binds a document, refusing it whole if any name in it is unknown.
     *
     * <p>⚠️ <strong>The requested budget is clamped, never refused.</strong> A document asking for more
     * than the ceiling loads and runs at the ceiling: refusing it would mean a workspace stops working
     * because somebody changed a number on a plan, which is a failure with no visible cause. What the
     * clamp guarantees is that the document's number cannot win.</p>
     *
     * @param document  the parsed document
     * @param requested what the document asked for — from wherever the host stores it, not from the
     *                  source; the dialect has no syntax for a budget and is not going to grow one
     * @return the bound script
     * @throws ScriptBindException when the document names something the host did not declare
     */
    public BoundScript bind(ScriptDocumentNode document, ScriptBudget requested) {
        try {
            return read(document, requested == null ? ScriptBudget.unlimited() : requested);
        } catch (ScriptBindException failure) {
            throw failure.in(document.getName());
        }
    }

    private BoundScript read(ScriptDocumentNode document, ScriptBudget requested) {
        Map<String, FunctionDeclarationNode> functions  = collectFunctions(document);
        Map<String, BehaviourNode>           behaviours = collectBehaviours(document);
        Map<String, List<HandlerNode>>       handlers   = new LinkedHashMap<>();
        CatalogueAudit                       audit      = auditFor(functions.keySet());

        for (ScriptNode script : document.getScripts()) {
            for (HandlerNode handler : script.getHandlers()) {
                bindHandler(handler, handlers, audit);
            }
        }

        for (FunctionDeclarationNode function : functions.values()) {
            auditBody(function, audit);
        }

        List<String> includes = document.getIncludes().stream().map(IncludeNode::getPath).toList();

        return new BoundScript(document.getName(), handlers, functions, behaviours, includes,
                               requested.clampTo(ceiling));
    }

    /**
     * Checks one handler's event and its expressions, then files it under the event it listens to.
     */
    private void bindHandler(HandlerNode handler, Map<String, List<HandlerNode>> handlers, CatalogueAudit audit) {
        SourceSpan at = ScriptSpanNode.at(handler);

        if (!catalogue.declaresEvent(handler.getEvent())) {
            throw new ScriptBindException(at, "'%s' is not an event this host fires; it declares %s"
                    .formatted(handler.getEvent(), catalogue.eventNames()));
        }

        audit.audit(handler.getCondition(), at);
        auditBody(handler, audit);

        handlers.computeIfAbsent(handler.getEvent(), event -> new ArrayList<>()).add(handler);
    }

    /**
     * Gathers every function the document declares, in a script block or in a behaviour.
     *
     * <p>⚠️ <strong>A duplicate name is refused rather than overwritten.</strong> Two functions called
     * {@code hostile} in one file is somebody editing a copy of a block they meant to rename; letting
     * the second win silently means half the file calls a function nobody can see in the other half.</p>
     */
    private Map<String, FunctionDeclarationNode> collectFunctions(ScriptDocumentNode document) {
        Map<String, FunctionDeclarationNode> functions = new LinkedHashMap<>();

        for (ScriptNode script : document.getScripts()) {
            script.getFunctions().forEach(function -> declare(functions, function));
        }

        for (BehaviourNode behaviour : document.getBehaviours()) {
            behaviour.getFunctions().forEach(function -> declare(functions, function));
        }

        return functions;
    }

    private void declare(Map<String, FunctionDeclarationNode> functions, FunctionDeclarationNode function) {
        FunctionDeclarationNode declared = functions.putIfAbsent(function.getName(), function);

        if (declared != null) {
            throw new ScriptBindException(
                    ScriptSpanNode.at(function),
                    "'%s' is declared twice in this document; the first is at %s"
                            .formatted(function.getName(), ScriptSpanNode.at(declared))
            );
        }
    }

    private Map<String, BehaviourNode> collectBehaviours(ScriptDocumentNode document) {
        Map<String, BehaviourNode> behaviours = new LinkedHashMap<>();

        for (BehaviourNode behaviour : document.getBehaviours()) {
            BehaviourNode declared = behaviours.putIfAbsent(behaviour.getName(), behaviour);

            if (declared != null) {
                throw new ScriptBindException(
                        ScriptSpanNode.at(behaviour),
                        "a behaviour called '%s' is declared twice in this document; the first is at %s"
                                .formatted(behaviour.getName(), ScriptSpanNode.at(declared))
                );
            }
        }

        return behaviours;
    }

    /**
     * The names an expression in this document may call: what the host declared, what the script
     * declared, and what the dialect provides on its own.
     */
    private CatalogueAudit auditFor(Set<String> declared) {
        Set<String> functions = new LinkedHashSet<>(catalogue.functionNames());

        functions.addAll(declared);
        functions.addAll(ScriptExtension.builtinFunctions());

        return new CatalogueAudit(catalogue, Set.copyOf(functions), filters, tests);
    }

    /**
     * Walks a body statement by statement, auditing every expression it holds.
     *
     * <p>⚠️ <strong>A statement shape this method does not know is refused.</strong> The alternative —
     * ignoring it and moving on — is an audit that reports a document clean because it walked past the
     * part it did not recognise, which is the one outcome worse than refusing a valid file.</p>
     */
    private void auditBody(ScriptBodyNode body, CatalogueAudit audit) {
        SourceSpan at = position(body, SourceSpan.none());

        for (Expression statement : body.getExpressions()) {
            auditStatement(statement, audit, at);
        }
    }

    private void auditStatement(Expression statement, CatalogueAudit audit, SourceSpan enclosing) {
        SourceSpan at = position(statement, enclosing);

        switch (statement) {
            case ScriptBodyNode body -> auditBody(body, audit);
            case IfNode branch -> auditBranches(branch, audit, at);
            case LocalNode local -> audit.audit(local.getValue(), at);
            case AssignmentNode assignment -> audit.audit(assignment.getValue(), at);
            case ReturnNode returned -> audit.audit(returned.getValue(), at);
            default -> {
                requireEffect(statement, at);
                audit.audit(statement, at);
            }
        }

        if (statement instanceof ForNode loop) {
            audit.audit(loop.getIterable(), at);
        }
    }

    private void auditBranches(IfNode branch, CatalogueAudit audit, SourceSpan enclosing) {
        SourceSpan at = position(branch, enclosing);

        for (IfBranchNode alternative : branch.getBranches()) {
            audit.audit(alternative.getCondition(), at);

            for (Expression statement : alternative.getExpressions()) {
                auditStatement(statement, audit, at);
            }
        }
    }

    /**
     * Refuses a statement that computes something and does nothing with it.
     *
     * <p>⚠️ <strong>A statement in this language is a call, or it is a mistake.</strong> Everything
     * else a body may hold — {@code if}, {@code for}, {@code local}, {@code return}, an assignment, a
     * nested declaration — is handled by the switch above, so what reaches here is a bare expression:
     * {@code entry.weight} on a line of its own, {@code @player:$id}, a leftover {@code 5}. Every one of
     * them parses, evaluates, throws the answer away and reports nothing.</p>
     *
     * <p>That is the same silent failure the {@code ==} check in {@code AssignmentParser} exists to
     * prevent, arriving through a different door: somebody deletes half a line, the file still loads,
     * and the script quietly stops doing one of the things it says it does. Refusing costs one branch
     * and turns it into a sentence at load.</p>
     *
     * @param statement the statement to judge
     * @param at        where it was written
     */
    private void requireEffect(Expression statement, SourceSpan at) {
        boolean call = statement instanceof FunctionNode
                || statement instanceof BeanAccessNode access
                   && access.getAction() instanceof BeanAccessNode.MethodCall;

        if (!call) {
            throw new ScriptBindException(at, ("'%s' computes a value and does nothing with it; a "
                    + "statement is a call, an assignment, or one of if / for / local / return")
                    .formatted(statement.toSource()));
        }
    }

    /**
     * Where to say a failure happened.
     *
     * <p>⚠️ <strong>A statement is not guaranteed to carry a span.</strong> The dialect's own nodes are
     * built by parsers that stamp one; a bare facade call is an {@code jmouse-el} node built by the
     * expression parser, which does not. Falling back to the construction that <em>holds</em> the
     * statement names the handler rather than the line — still a place to open the file at, where the
     * alternative is {@code 0:0} and a reader with nothing to go on.</p>
     */
    private SourceSpan position(Expression expression, SourceSpan enclosing) {
        SourceSpan at = ScriptSpanNode.at(expression);

        return at.isKnown() ? at : enclosing;
    }
}
