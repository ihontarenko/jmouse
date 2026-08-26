package org.jmouse.mapper.el;

import org.jmouse.core.access.descriptor.structured.DescriptorResolver;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.SpanNode;
import org.jmouse.mapper.binding.MappingAssertion;
import org.jmouse.mapper.binding.PropertyMapping;
import org.jmouse.mapper.binding.TypeMappingRule;
import org.jmouse.mapper.el.node.AssertionNode;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.IncludeNode;
import org.jmouse.mapper.el.node.RefuseNode;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.parser.JmmSyntaxException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a parsed document into the rules the mapping engine already understands. 🔗
 *
 * <p>This is where the language stops being a language. Past this point there is no document, no
 * nodes and no text — only {@link TypeMappingRule}s, indistinguishable from the ones the Java DSL and
 * the annotations produce, which is what makes {@code .jmm} a third way of saying the same thing rather
 * than a second engine.</p>
 *
 * <h2>⚠️ The expression language used here knows nothing of .jmm</h2>
 *
 * <p>A plain {@link ExpressionLanguage} — deliberately not one wired to
 * {@link org.jmouse.mapper.el.lexer.JmmRecognizer}. The reader sliced every value out as text precisely
 * so that it could be lexed by something that reads {@code source}, {@code target} and {@code from} as
 * the identifiers they are inside an expression. Compiling them here with the document's own lexer
 * would reintroduce the failure the slicing exists to avoid.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmBinder {

    private final ExpressionLanguage language;
    private final ClassLoader        loader;
    private final Conversion         conversion;

    public JmmBinder() {
        this(new ExpressionLanguage(), Thread.currentThread().getContextClassLoader());
    }

    public JmmBinder(ExpressionLanguage language, ClassLoader loader) {
        this(language, loader, null);
    }

    /**
     * A binder whose expressions resolve converters against a conversion the caller supplies.
     *
     * <h3>⚠️ Why this has to be given rather than found</h3>
     *
     * <p>{@link ExpressionLanguage#newContext()} builds a <strong>fresh</strong>
     * {@code ExpressionLanguageConversion} every call, so there is nowhere for anybody to register a
     * named converter — which would make {@code value | via("name")} a filter that can never find
     * anything. Handing the binder the conversion the mapping engine itself uses is what makes the two
     * agree about which converters exist; two registries would differ silently, and the symptom would
     * be a name that resolves in Java and not in a file.</p>
     *
     * <p>⚠️ Optional, and {@code null} keeps the old behaviour exactly — the language's own conversion,
     * built per evaluation. Nothing that worked before this is wired differently by accident.</p>
     *
     * @param language   the expression language
     * @param loader     what resolves the types a file imports
     * @param conversion the conversion whose converters expressions may reach, or {@code null}
     */
    public JmmBinder(ExpressionLanguage language, ClassLoader loader, Conversion conversion) {
        this.language = language;
        this.loader = loader;
        this.conversion = conversion;

        // ⚠️ Contributed here rather than shipped with the language, because it reads `source` — a name
        // this binder binds and jmouse-el knows nothing about. The FILTER form needs no such convention
        // and does live in the language; see ViaFunction for the split.
        language.getExtensions().addFunction(new ViaFunction());
    }

    /**
     * Binds every pair the document declares into a rule source.
     *
     * @param document the parsed file
     * @param source   where the rules are registered
     */
    public void bind(MappingDocumentNode document, JmmRuleSource source) {
        for (TargetNode target : document.getTargets()) {
            Class<?> targetType = resolve(document, target.getTargetType(), target.getSpan());

            for (FromNode from : target.getSources()) {
                Class<?> sourceType = resolve(document, from.getSourceType(), from.getSpan());

                if (from.isConverted()) {
                    source.register(sourceType, targetType,
                                    converting(target, from, sourceType, targetType));
                    continue;
                }

                Map<String, PropertyMapping> mappings = rulesFor(document, target, from, sourceType);

                JmmValidator.validate(document, target, from, sourceType, targetType, mappings.keySet());
                source.register(sourceType, targetType,
                                new TypeMappingRule(sourceType, targetType, mappings,
                                                    assertionsFor(target, from, sourceType, targetType)));
            }
        }
    }

    /**
     * Builds the rule for a {@code from X : <expression>} — a pair converted whole.
     *
     * <h3>⚠️ What this block may and may not carry</h3>
     *
     * <p>Refused here rather than at runtime, because both would otherwise be statements the file makes
     * and the engine silently does not honour — the one failure this language was shaped against:</p>
     *
     * <ul>
     *   <li>{@code unmapped fail} — nothing is filled property by property, so there is no unmapped
     *       property for it to be about. A file asking for it is asking for a check that cannot run.</li>
     *   <li>{@code refuse target before} — it fires only for a caller-supplied instance, and a
     *       caller-supplied instance is <strong>discarded</strong> here: the expression produces the
     *       object, so there is nothing to write into and nothing to inspect first.</li>
     * </ul>
     *
     * <p>{@code refuse source before} and {@code refuse target after} both carry over unchanged — the
     * source is unchanged by any of this, and the target exists by the time the second one runs.</p>
     *
     * @param target     the target block
     * @param from       the source block, whose conversion expression this is
     * @param sourceType the resolved source type
     * @param targetType the resolved target type
     * @return the rule
     */
    private TypeMappingRule converting(
            TargetNode target,
            FromNode from,
            Class<?> sourceType,
            Class<?> targetType
    ) {
        if (target.getUnmapped() == TargetNode.Unmapped.FAIL) {
            throw JmmSyntaxException.at(from.getSpan(), ("'target %s' converts '%s' whole and also says "
                    + "'unmapped fail', which cannot hold: nothing is filled property by property, so "
                    + "there is no unmapped property to fail on")
                    .formatted(target.getTargetType(), from.getSourceType()));
        }

        for (RefuseNode refusal : target.getRefusals()) {
            if (refusal.getSubject() == RefuseNode.Subject.TARGET
                    && refusal.getPhase() == RefuseNode.Phase.BEFORE) {
                throw JmmSyntaxException.at(refusal.getSpan(), ("'target %s' converts '%s' whole, so 'refuse "
                        + "target before' can never run: it checks an instance the caller supplied, and "
                        + "a converted pair discards one: the expression produces the object. Use "
                        + "'refuse source before' or 'refuse target after'")
                        .formatted(target.getTargetType(), from.getSourceType()));
            }
        }

        String     expression = from.getConversion();
        Expression compiled   = compile(expression, from.getSpan());

        return TypeMappingRule.converting(
                sourceType, targetType,
                assertionsFor(target, from, sourceType, targetType),
                new TypeMappingRule.WholeTargetMapping(
                        expression,
                        (value, context) -> evaluate(
                                compiled, value, BlockBindings.NONE, sourceType, Object.class)));
    }

    /**
     * Builds what refuses this pair.
     *
     * <p>⚠️ A source assertion is evaluated against the <em>source</em> and a target assertion against
     * the <em>target</em>, so each compiles against a different type and each binds that type's
     * properties into the context. Getting this backwards would produce a condition that reads nothing
     * and is therefore quietly false — and a refusal that never fires is worse than none, because the
     * file says the check is there.</p>
     *
     * @param target     the target block, for its target-level refusals
     * @param from       the source block, for its source refusal
     * @param sourceType the resolved source type
     * @param targetType the resolved target type
     * @return the assertions, in the order written
     */
    private List<MappingAssertion> assertionsFor(
            TargetNode target,
            FromNode from,
            Class<?> sourceType,
            Class<?> targetType
    ) {
        List<MappingAssertion> assertions = new ArrayList<>();

        for (RefuseNode refusal : target.getRefusals()) {
            collect(refusal, targetType, assertions);
        }

        if (from.getRefusal() != null) {
            collect(from.getRefusal(), sourceType, assertions);
        }

        return assertions;
    }

    /**
     * Compiles one refusal block against the type it is about.
     *
     * @param refusal the block
     * @param against the type its conditions read
     * @param into    where the assertions are collected
     */
    private void collect(RefuseNode refusal, Class<?> against, List<MappingAssertion> into) {
        MappingAssertion.Subject subject = refusal.getSubject() == RefuseNode.Subject.SOURCE
                ? MappingAssertion.Subject.SOURCE
                : MappingAssertion.Subject.TARGET;
        MappingAssertion.Phase phase = refusal.getPhase() == RefuseNode.Phase.BEFORE
                ? MappingAssertion.Phase.BEFORE
                : MappingAssertion.Phase.AFTER;

        for (AssertionNode assertion : refusal.getAssertions()) {
            Expression condition = compile(assertion.getCondition(), assertion.getSpan());

            // ⚠️ No bindings, and that is unchanged rather than decided here: a refusal reads the object
            // it is about and has never seen the enclosing block's `let` names. Worth knowing before
            // writing one that wants a binding — it will not resolve.
            into.add(new MappingAssertion(subject, phase,
                                          (value, context) -> Boolean.TRUE.equals(evaluate(
                                                  condition, value, BlockBindings.NONE, against, Boolean.class)),
                                          assertion.getMessage()));
        }
    }

    /**
     * Merges everything that applies to one pair, in precedence order.
     *
     * <p>Later writes lose: {@code from} is applied last only in the sense that it is put in first, and
     * {@code putIfAbsent} keeps it. The order here is the order the decision document states — a rule in
     * {@code from}, then what {@code from} includes, then {@code always}, then what {@code always}
     * includes.</p>
     *
     * @param document   the file, for its fragments
     * @param target     the target block
     * @param from       the source block
     * @param sourceType the resolved source type
     * @return every mapping that applies, keyed by target property
     */
    private Map<String, PropertyMapping> rulesFor(
            MappingDocumentNode document,
            TargetNode target,
            FromNode from,
            Class<?> sourceType
    ) {
        Map<String, PropertyMapping> mappings = new LinkedHashMap<>();

        apply(document, from.getRules(), sourceType, mappings);

        if (target.getAlways() != null) {
            apply(document, target.getAlways(), sourceType, mappings);
        }

        return mappings;
    }

    /**
     * Applies one block, then whatever it includes.
     *
     * @param document   the file, for its fragments
     * @param block      the block to apply
     * @param sourceType the resolved source type
     * @param mappings   what has been decided so far
     */
    private void apply(
            MappingDocumentNode document,
            RuleBlockNode block,
            Class<?> sourceType,
            Map<String, PropertyMapping> mappings
    ) {
        if (block == null) {
            return;
        }

        applyRules(block, sourceType, mappings);

        for (IncludeNode include : block.getIncludes()) {
            RuleBlockNode fragment = document.getFragments().get(include.getName());

            if (fragment == null) {
                throw JmmSyntaxException.at(include.getSpan(), ("no fragment called '%s' is declared in "
                        + "this file").formatted(include.getName()));
            }

            applyRules(fragment, sourceType, mappings);
        }
    }

    /**
     * Applies one block's own rules, against one block's own bindings.
     *
     * <p>⚠️ The bindings are compiled <strong>here</strong>, once for the block, and not inside the
     * lambda each rule hands the engine. They used to be compiled per evaluation — that is per property
     * per mapped object — which meant a block with two bindings and four computed rules ran eight full
     * compilations for every object that went through it, to arrive at expressions whose text cannot
     * change.</p>
     *
     * <p>A fragment is a block like any other and gets its own, which is why this exists as a method
     * rather than as two copies of a loop.</p>
     *
     * @param block      the block whose rules to apply
     * @param sourceType the resolved source type
     * @param mappings   what has been decided so far
     */
    private void applyRules(RuleBlockNode block, Class<?> sourceType, Map<String, PropertyMapping> mappings) {
        BlockBindings bindings = BlockBindings.of(
                block.getBindings(), binding -> compile(binding.getExpression(), binding.getSpan()));

        for (RuleNode rule : block.getRules().values()) {
            mappings.putIfAbsent(rule.getProperty(), toMapping(rule, bindings, sourceType));
        }
    }

    /**
     * Turns one rule into the mapping the engine executes.
     *
     * @param rule       the rule
     * @param bindings   the {@code let} bindings its block declared
     * @param sourceType the resolved source type
     * @return the mapping
     */
    private PropertyMapping toMapping(RuleNode rule, BlockBindings bindings, Class<?> sourceType) {
        String name = rule.getProperty();

        if (rule.isIgnored()) {
            return PropertyMapping.ignore(name);
        }

        // ⚠️ Asked of THIS path, not of the block. The test used to be "is it a path AND does this block
        // bind nothing", which made a single `let` — written for one property — quietly move every other
        // rule in the block off the accessor and onto the expression machinery. What decides it is
        // whether this path's own root is a bound name, and SourcePath is where both halves of the
        // language now ask that; see the note there on why the validator has to ask the same one.
        PropertyMapping mapping = SourcePath.readsSource(rule.getValue(), bindings.names())
                ? PropertyMapping.reference(name, rule.getValue())
                : computed(name, rule.getValue(), bindings, sourceType, rule.getSpan());

        if (rule.getCondition() == null) {
            return mapping;
        }

        Expression condition = compile(rule.getCondition(), rule.getSpan());

        // ⚠️ Guarded, not When. `when` in this language means the property is left exactly as it was,
        // and PropertyMapping.When means "yield null and let the global null policy decide" — a
        // different outcome that happens to look the same under one of the three policies. When is
        // programmatic API with callers of its own; bending it to this language's meaning would change
        // what those callers do, silently, and neither reading is wrong for its own audience.
        return PropertyMapping.guarded(
                name,
                (value, context) -> Boolean.TRUE.equals(
                        evaluate(condition, value, bindings, sourceType, Boolean.class)),
                mapping);
    }

    /**
     * Builds a mapping that evaluates an expression against the source.
     *
     * @param name       the target property
     * @param expression the expression, as written
     * @param bindings   the block's bindings
     * @param sourceType the resolved source type
     * @param span       where the rule was written, so a value that will not compile names a line
     * @return the mapping
     */
    private PropertyMapping computed(
            String name,
            String expression,
            BlockBindings bindings,
            Class<?> sourceType,
            SpanNode span
    ) {
        Expression compiled = compile(expression, span);

        // ⚠️ Expression, not Compute. A Compute is a lambda and nothing else, so the moment a rule
        // becomes one, what the file said is gone — a translator rendering the document back to source,
        // or into JSON, would have nothing to render but "a function". The text travels beside the
        // behaviour, and the engine still evaluates only the behaviour.
        return PropertyMapping.expression(
                name, expression,
                (value, context) -> evaluate(compiled, value, bindings, sourceType, Object.class));
    }

    /**
     * Evaluates a compiled expression against one source object.
     *
     * <p>⚠️ A context is built per call, and the source's readable properties are bound into it so that
     * a bare {@code buyer.email} resolves the way the language says it does. That is a real cost — it is
     * paid per computed property per object — and it is the price of bare paths being the default
     * spelling. A rule that is only a path never reaches here; it became a
     * {@link PropertyMapping.Reference} instead.</p>
     *
     * @param expression the compiled expression
     * @param source     the object being mapped
     * @param bindings   the block's bindings, already compiled, evaluated in declaration order
     * @param sourceType the resolved source type
     * @param type       what the caller wants back
     * @param <T>        the result type
     * @return the value
     */
    private <T> T evaluate(
            Expression expression,
            Object source,
            BlockBindings bindings,
            Class<?> sourceType,
            Class<T> type
    ) {
        EvaluationContext context = newContext(source, sourceType);

        // ⚠️ Evaluated here and COMPILED at bind time. This loop used to call the compiler, so the whole
        // lex-parse-build pipeline ran for every binding of every computed property of every object.
        bindings.into(context);

        return context.getConversion().convert(expression.evaluate(context), type);
    }

    /**
     * A context for one evaluation, over the object it is about.
     *
     * <p>⚠️ The source is the context's outermost scope rather than something copied into it — see
     * {@link SourceValues}. What used to stand here read every readable property of the source before
     * the expression was given a chance to say which two it wanted, once for every computed rule of
     * every object.</p>
     *
     * @param source     the object being mapped
     * @param sourceType the resolved source type
     * @return the context
     */
    private EvaluationContext newContext(Object source, Class<?> sourceType) {
        SourceScopedChain chain = new SourceScopedChain(new SourceValues(source, describe(sourceType)));

        // ⚠️ The language's own conversion where the caller supplied none, so that nothing which worked
        // before this is wired differently by accident — see the constructor's note on `via`.
        Conversion resolved = conversion == null ? language.newContext().getConversion() : conversion;

        return new DefaultEvaluationContext(chain, language.getExtensions(), resolved);
    }

    /**
     * Describes a source type through the mapper's own descriptor cache.
     *
     * @param sourceType the type to describe
     * @return its description
     */
    @SuppressWarnings("unchecked")
    private ObjectDescriptor<Object> describe(Class<?> sourceType) {
        return (ObjectDescriptor<Object>) DescriptorResolver.describe(sourceType);
    }

    /**
     * Compiles an expression, keeping whatever the compiler said when it cannot — and where it was
     * written.
     *
     * <p>⚠️ The cause travels with it. The compiler's message is folded into this one and used to be all
     * that survived; the stack under it, which is the only thing that says where inside the compiler the
     * refusal came from, was dropped.</p>
     *
     * @param expression the expression, as written
     * @param span       where in the file it was written, or {@code null}
     * @return the compiled form
     */
    private Expression compile(String expression, SpanNode span) {
        try {
            return language.compile(expression);
        } catch (RuntimeException failure) {
            throw JmmSyntaxException.at(
                    span, "cannot compile '%s': %s".formatted(expression, failure.getMessage()), failure);
        }
    }

    /**
     * Resolves a type name the file used.
     *
     * @param document   the file, for its imports
     * @param simpleName the name as written
     * @param span       the construction that named it, so a missing class names a line
     * @return the class
     */
    private Class<?> resolve(MappingDocumentNode document, String simpleName, SpanNode span) {
        String qualified = document.resolve(simpleName);

        try {
            return Class.forName(qualified, false, loader);
        } catch (ClassNotFoundException notFound) {
            List<String> known = new ArrayList<>(document.getImports().keySet());

            throw JmmSyntaxException.at(span, ("no class called '%s'. Imported in this file: %s")
                    .formatted(qualified, known.isEmpty() ? "nothing" : String.join(", ", known)), notFound);
        }
    }
}
