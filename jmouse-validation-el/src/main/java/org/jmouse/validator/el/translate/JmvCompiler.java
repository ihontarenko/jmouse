package org.jmouse.validator.el.translate;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.validator.constraint.StandardConstraintModule;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;
import org.jmouse.validator.el.node.CheckBlockNode;
import org.jmouse.validator.el.node.CheckLineNode;
import org.jmouse.validator.el.node.CheckNode;
import org.jmouse.validator.el.node.InvariantNode;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.node.WhenBranchNode;
import org.jmouse.validator.el.node.WhenNode;
import org.jmouse.validator.el.runtime.CheckSignature;
import org.jmouse.validator.el.runtime.CheckSignatures;
import org.jmouse.validator.el.runtime.CompiledCheck;
import org.jmouse.validator.el.runtime.CompiledGuard;
import org.jmouse.validator.el.runtime.CompiledInvariant;
import org.jmouse.validator.el.runtime.CompiledItem;
import org.jmouse.validator.el.runtime.CompiledLine;
import org.jmouse.validator.el.runtime.CompiledValidation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a parsed {@code .jmv} into something that judges records. ⚙️
 *
 * <h2>⚠️ It is a {@link org.jmouse.el.translate.Translator}, not a class called a compiler</h2>
 *
 * <p>Compiling a tree for a backend and rendering a tree back into text are the same operation with
 * different destinations, and the engine says so in one seam. Two names for it is how an un-parse and a
 * compiler drift apart until a document written by one and read by the other means two things — so
 * this and {@code JmvWriter} implement the same interface, and neither is privileged.</p>
 *
 * <h2>⚠️ Two expression languages, deliberately</h2>
 *
 * <p>The one that read the <em>file</em> knows this language's keywords. The one passed here compiles
 * what was <em>sliced out of</em> the file and must not: a guard reading {@code stop_reason == 'none'}
 * is only parseable by a lexer that has never heard of {@code stop}. Keeping them apart is also what
 * lets a product widen what a condition may use without widening what a document may say.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmvCompiler implements ValidationTranslator<CompiledValidation> {

    private final ExpressionLanguage     expressionLanguage;
    private final ConstraintTypeRegistry registry;
    private final CheckSignatures        signatures;
    private final Mapper                 mapper;

    /** A compiler over the standard vocabulary — every shipped constraint and check. */
    public JmvCompiler() {
        this(new ExpressionLanguage(), standardRegistry(), CheckSignatures.standard(),
             Mappers.defaultMapper());
    }

    public JmvCompiler(
            ExpressionLanguage expressionLanguage,
            ConstraintTypeRegistry registry,
            CheckSignatures signatures,
            Mapper mapper
    ) {
        this.expressionLanguage = expressionLanguage;
        this.registry = registry;
        this.signatures = signatures;
        this.mapper = mapper;
    }

    /**
     * ⚠️ Everything, because this destination evaluates the document itself and has nothing it cannot
     * reach. A translator rendering into somebody else's evaluator declares less, and is refused for
     * the difference.
     */
    @Override
    public Capabilities capabilities() {
        return JmvCapability.everything("jmv-runtime");
    }

    /**
     * Compiles a document.
     *
     * @param node     the parsed document
     * @param bindings ⚠️ unused, and named so rather than removed: the seam is shared, and a
     *                 destination that binds values at translation time — SQL does — needs it. This one
     *                 binds per record instead, because a validation is applied many times and the
     *                 record is what changes.
     * @return the compiled document
     */
    @Override
    public CompiledValidation translate(Node node, Bindings bindings) {
        if (!(node instanceof ValidationDocumentNode document)) {
            throw new TranslationRefusedException(
                    "the jMV runtime translates a validation document, and was handed "
                    + (node == null ? "nothing" : node.getClass().getSimpleName()));
        }

        requireSupport(document.getExpressions());

        List<CompiledItem> gate = document.getGate()
                .map(block -> compile(block.getExpressions()))
                .orElseGet(List::of);

        List<CompiledItem> body = new ArrayList<>();

        for (Expression statement : document.getExpressions()) {
            if (statement instanceof CheckBlockNode block) {
                // ⚠️ `always` is flattened and `gate` is taken out. The words group lines for a reader
                // and change nothing about when they run; a compiled tree preserving them would invite
                // a later reader to believe they did.
                if (block.getKind() == CheckBlockNode.Kind.ALWAYS) {
                    body.addAll(compile(block.getExpressions()));
                }

                continue;
            }

            body.add(compile(statement));
        }

        return new CompiledValidation(document.getName(), gate, body, expressionLanguage, registry,
                                      mapper);
    }

    /**
     * Compiles a list of statements.
     *
     * @param statements what to compile
     * @return the compiled items
     */
    private List<CompiledItem> compile(List<Expression> statements) {
        List<CompiledItem> items = new ArrayList<>(statements.size());

        for (Expression statement : statements) {
            items.add(compile(statement));
        }

        return items;
    }

    /**
     * Compiles one statement.
     *
     * @param statement what to compile
     * @return the compiled item
     */
    private CompiledItem compile(Expression statement) {
        return switch (statement) {
            case CheckLineNode line -> compileLine(line);
            case WhenNode guard -> compileGuard(guard);
            case InvariantNode invariant -> new CompiledInvariant(
                    compileExpression(invariant.getCondition()),
                    compileExpression(invariant.getMessage()));
            case CheckBlockNode block -> new CompiledGuard(
                    compileExpression("true"), compile(block.getExpressions()), List.of());
            default -> throw new TranslationRefusedException(
                    "'%s' is not something a validation document holds"
                            .formatted(statement.getClass().getSimpleName()));
        };
    }

    /**
     * Compiles a {@code when}, taking its branches apart into the guarded one and the other.
     *
     * <p>The parser models branches the way {@code if} does — a list, the unguarded one last — which is
     * right for reading a file. The runtime wants the two halves named, because it has to record which
     * of them did not run.</p>
     *
     * @param guard the parsed guard
     * @return the compiled guard
     */
    private CompiledGuard compileGuard(WhenNode guard) {
        Expression         condition = null;
        List<CompiledItem> body      = List.of();
        List<CompiledItem> otherwise = List.of();

        for (WhenBranchNode branch : guard.getBranches()) {
            if (branch.isGuarded()) {
                condition = compileExpression(branch.getCondition());
                body = compile(branch.getExpressions());

                continue;
            }

            otherwise = compile(branch.getExpressions());
        }

        if (condition == null) {
            throw new TranslationRefusedException("a 'when' reached the compiler with no condition");
        }

        return new CompiledGuard(condition, body, otherwise);
    }

    /**
     * Compiles one check line.
     *
     * @param line the parsed line
     * @return the compiled line
     */
    private CompiledLine compileLine(CheckLineNode line) {
        List<CompiledCheck> checks = new ArrayList<>(line.getChecks().size());

        for (CheckNode check : line.getChecks()) {
            // ⚠️ `optional` builds nothing. Every constraint already treats null as valid, so the word
            // says something to a reader and to a form-builder, and has nothing to ask of a value.
            if (CheckSignatures.isMarker(check.getName())) {
                continue;
            }

            checks.add(compileCheck(check));
        }

        return new CompiledLine(line.getField(), checks, compileExpression(line.getMessage()));
    }

    /**
     * Compiles one check, matching its arguments to the properties its constraint binds.
     *
     * @param check the parsed check
     * @return the compiled check
     */
    private CompiledCheck compileCheck(CheckNode check) {
        CheckSignature signature = signatures.resolve(check.getName()).orElseThrow(
                () -> new TranslationRefusedException(
                        ("there is no check called '%s'. Available: %s")
                                .formatted(check.getName(), signatures.available())));

        Map<String, List<Expression>> arguments = new LinkedHashMap<>();

        bindPositional(check, signature, arguments);

        check.getNamed().forEach(
                (key, expression) -> arguments.put(key, List.of(compileExpression(expression))));

        return new CompiledCheck(signature, arguments, compileExpression(check.getMessage()),
                                 check.isStop());
    }

    /**
     * Matches arguments given by position to the properties they fill.
     *
     * <p>⚠️ Too many is refused, naming the count. A check quietly ignoring its fourth argument is a
     * rule that validates less than it reads as validating — which is the one failure a validation
     * language must not have.</p>
     *
     * @param check     the parsed check
     * @param signature what its arguments mean
     * @param arguments where to put them
     */
    private void bindPositional(
            CheckNode check, CheckSignature signature, Map<String, List<Expression>> arguments) {

        List<String> given = check.getPositional();

        if (given.size() > signature.arity()) {
            throw new TranslationRefusedException(
                    ("'%s' takes %d argument(s) by position — %s — and was given %d")
                            .formatted(check.getName(), signature.arity(),
                                       String.join(", ", signature.positional()), given.size()));
        }

        if (signature.variadic()) {
            arguments.put(signature.positional().getFirst(),
                          given.stream().map(this::compileExpression).toList());

            return;
        }

        for (int index = 0; index < given.size(); index++) {
            arguments.put(signature.positional().get(index),
                          List.of(compileExpression(given.get(index))));
        }
    }

    /**
     * Compiles one sliced expression, or answers {@code null} for one that was never written.
     *
     * @param source the expression as it was typed
     * @return the compiled expression, or {@code null}
     */
    private Expression compileExpression(String source) {
        return source == null ? null : expressionLanguage.compile(source);
    }

    /**
     * The constraints the library ships with.
     *
     * @return a registry holding every standard constraint
     */
    private static ConstraintTypeRegistry standardRegistry() {
        ConstraintTypeRegistry registry = new ConstraintTypeRegistry();

        StandardConstraintModule.registerDefaults(registry);

        return registry;
    }
}
