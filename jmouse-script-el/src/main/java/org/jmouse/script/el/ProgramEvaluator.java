package org.jmouse.script.el;

import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.template.Cache;
import org.jmouse.script.el.DeclaredFunctions;
import org.jmouse.script.el.Facades;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.SourceSpan;
import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.el.budget.ScriptExecution;
import org.jmouse.script.el.lexer.StatementRecognizer;
import org.jmouse.script.el.node.FunctionDeclarationNode;
import org.jmouse.script.el.parser.ScriptBodyParser;
import org.jmouse.script.el.node.ProgramNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Reads and runs jMS programs — documents of top-level statements that return a value, with no event handlers.
 *
 * <pre>
 * local total = 0
 * for n in 1..k do
 *   total = total + 1 / n^2
 * end
 * return sqrt(6 * total)          # → π as k grows
 * </pre>
 *
 * <p>A run is given its <strong>variables</strong> (the inputs), its <strong>facades</strong> (what
 * {@code @name.method()} may reach — a configuration, a data source; nothing else is reachable) and its
 * <strong>budget</strong> (steps, loop iterations, recursion depth, a deadline). All three travel in the evaluation
 * context; there is no host object. The same document with {@code on … do … end} handlers is a script a host
 * drives through {@link ScriptEvaluator}, reaching its facades the same way.</p>
 *
 * <p>⚠️ An include contributes its <em>functions</em> only, never its statements, and is resolved through the
 * source function this evaluator was built with; without one, a program that includes anything is refused.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ProgramEvaluator {

    private static final String ANONYMOUS = "anonymous";

    private final ExtensionContainer                 extensions;
    private final Lexer                              lexer;
    private final ParserContext                      context;
    private final Cache<Cache.Key, ProgramNode>      cache;
    private final Function<String, Optional<String>> includes;

    /** A standard evaluator whose programs may not include other files. */
    public ProgramEvaluator() {
        this(path -> Optional.empty());
    }

    /** An evaluator that resolves {@code include 'path'} to a source through the given function. */
    public ProgramEvaluator(Function<String, Optional<String>> includes) {
        StandardExtensionContainer container = new StandardExtensionContainer();
        container.importExtension(new ScriptExtension());
        this.extensions = container;
        this.lexer = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new StatementRecognizer()));
        this.context = new DefaultParserContext(extensions);
        this.cache = Cache.memory();
        this.includes = includes;
    }

    public ProgramNode parse(String source) {
        return parse(source, ANONYMOUS);
    }

    public ProgramNode parse(String source, String name) {
        Cache.Key   key    = Cache.Key.forObject(name + ' ' + source);
        ProgramNode cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        ProgramNode program = read(source, name);
        cache.put(key, program);
        return program;
    }

    /** A fresh context over this evaluator's language, for a caller that sets more up itself. */
    public EvaluationContext newContext() {
        return new DefaultEvaluationContext(extensions);
    }

    /** Runs a program with no inputs, no facades and no limits. */
    public Object run(ProgramNode program) {
        return run(program, Map.of(), Facades.none(), ScriptBudget.unlimited());
    }

    /**
     * Runs a program once: binds its inputs, makes its functions (and its includes' functions) callable, installs
     * its facades and its budget, evaluates its statements, and answers what it returned — or the value of its last
     * statement.
     */
    public Object run(ProgramNode program, Map<String, ?> variables, Facades facades, ScriptBudget budget) {
        DefaultEvaluationContext evaluation = new DefaultEvaluationContext(extensions);
        variables.forEach(evaluation::setValue);
        evaluation.setBeanLookup(facades);
        DeclaredFunctions.of(functionsOf(program)).installInto(evaluation);
        // An unlimited budget begins no execution at all: nothing is counted, so nothing is installed.
        ScriptExecution execution = ScriptExecution.begin(program.getName(), budget);
        if (execution != null) {
            execution.installInto(evaluation);
        }
        try {
            return program.evaluate(evaluation);
        } finally {
            ScriptExecution.clear(evaluation);
        }
    }

    private Map<String, FunctionDeclarationNode> functionsOf(ProgramNode program) {
        Map<String, FunctionDeclarationNode> functions = new LinkedHashMap<>();
        for (String path : program.getIncludes()) {
            String source = includes.apply(path).orElseThrow(() -> new ScriptParseException(
                    SourceSpan.none().in(program.getName()),
                    "'%s' cannot be included here: this evaluator resolves no includes".formatted(path)));
            functions.putAll(functionsOf(parse(source, path)));
        }
        functions.putAll(program.getFunctions());
        return functions;
    }

    private ProgramNode read(String source, String name) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, source));
        cursor.consumeIf(BasicToken.T_SOL);
        Node body;
        try {
            body = context.getParser(ScriptBodyParser.class).parse(cursor, context);
        } catch (ScriptParseException failure) {
            throw failure.in(name);
        } catch (ParseException | SyntaxErrorException failure) {
            throw new ScriptParseException(new SourceSpan(name, Math.max(0, cursor.current().lineNumber()), 0), failure.getMessage());
        }
        if (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL)) {
            throw new ScriptParseException(new SourceSpan(name, Math.max(0, cursor.current().lineNumber()), 0),
                    "'%s' closes nothing here; the top level of a program is a list of statements".formatted(cursor.current().value()));
        }
        List<Expression> statements = body == null ? List.of() : body.getChildren().stream().map(Expression.class::cast).toList();
        return new ProgramNode(name, statements);
    }
}
