package org.jmouse.script.el;

import org.jmouse.core.access.AttributeResolver;
import org.jmouse.el.extension.*;
import org.jmouse.el.extension.attribute.JavaBeanAttributeResolver;
import org.jmouse.el.extension.attribute.ListAttributeResolver;
import org.jmouse.el.extension.attribute.MapAttributeResolver;
import org.jmouse.el.extension.filter.*;
import org.jmouse.el.extension.filter.converter.*;
import org.jmouse.el.extension.function.MaxFunction;
import org.jmouse.el.extension.function.MinFunction;
import org.jmouse.el.extension.function.mathematic.ExponentaFunction;
import org.jmouse.el.extension.function.mathematic.SquareRootFunction;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.*;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.*;
import org.jmouse.script.el.parser.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything the {@code .jms} language is made of, registered as one extension.
 *
 * <h2>⚠️ What is deliberately absent</h2>
 *
 * <p>This list is a <strong>closed vocabulary</strong>, not a convenience selection, and three things
 * the expression language offers are left out on purpose:</p>
 *
 * <ul>
 *   <li><strong>Reflection.</strong> {@code ClassFilter}, {@code ClassFunction} and
 *       {@code JavaReflectedFunction} turn a string into a {@link Class} and call into it. Any one of
 *       them makes the catalogue in {@link org.jmouse.script.el.host.ScriptCatalogue} decorative — a
 *       script that can name a class does not need a facade.</li>
 *   <li><strong>The wall clock.</strong> {@code NowFunction} reads the current time, which makes an
 *       expression answer differently on two machines given identical inputs. A host that wants a clock
 *       declares one as a facade, and then it is the host's clock — replayable, testable, and the same
 *       for everybody watching the same simulation.</li>
 *   <li><strong>{@code setVariable} / {@code getVariable}.</strong> The language already has
 *       {@code local} and assignment. A second way to write a variable is a second thing to explain,
 *       and it reaches the scope chain from inside an expression, where nobody looks for it.</li>
 * </ul>
 *
 * <p>What <em>is</em> present that {@code .jmp} refuses is {@link BeanAccessParser}. The two dialects
 * take opposite halves of one decision: a policy strips {@code @bean.method} so a rule cannot call
 * {@code @userRepository.deleteAll()}; a script keeps the syntax and closes the set of names it can
 * resolve. See {@link org.jmouse.script.el.host.FacadeLookup} for the half that has to hold.</p>
 *
 * <p>⚠️ The order of {@link #getParsers()} carries no meaning. Dispatch sorts by
 * {@link ParserPriority}, and that class — not this list — is where the reason each shape is offered
 * before another is written down.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptExtension implements Extension {

    /**
     * The dialect's own vocabulary, as names, computed once.
     *
     * <p>⚠️ <strong>Derived from the lists below rather than written out again.</strong> Two lists of
     * the same names is one list plus a defect, waiting for somebody to add a filter to only one.</p>
     *
     * <p>⚠️ And <strong>held</strong> rather than recomputed: the binder asks for all three, and each
     * question used to construct a whole {@code ScriptExtension} — thirty-odd parser objects included —
     * in order to read a handful of strings off it.</p>
     */
    private static final Set<String> FUNCTION_NAMES = namesOf(new ScriptExtension().getFunctions(), Function::getName);
    private static final Set<String> FILTER_NAMES   = namesOf(new ScriptExtension().getFilters(), Filter::getName);
    private static final Set<String> TEST_NAMES     = namesOf(new ScriptExtension().getTests(), Test::getName);

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return List.of(
                new JavaBeanAttributeResolver(),
                new MapAttributeResolver(),
                new ListAttributeResolver()
        );
    }

    @Override
    public List<Function> getFunctions() {
        return List.of(
                new SquareRootFunction(),
                new ExponentaFunction(),
                new MinFunction(),
                new MaxFunction()
        );
    }

    @Override
    public List<Filter> getFilters() {
        return List.of(
                new FilterFilter(),
                new MapFilter(),
                new LowerFilter(),
                new UpperFilter(),
                new SubFilter(),
                new DefaultFilter(),
                new TrimFilter(),
                new LengthFilter(),
                new SplitFilter(),
                new JoinFilter(),
                new LastFilter(),
                new FirstFilter(),
                // type-converters
                new ToBooleanFilter(),
                new ToByteFilter(),
                new ToShortFilter(),
                new ToIntFilter(),
                new ToLongFilter(),
                new ToFloatFilter(),
                new ToDoubleFilter(),
                new ToCharacterFilter(),
                new ToStringFilter(),
                new ToListFilter(),
                new ToArrayFilter(),
                new ToIteratorFilter()
        );
    }

    @Override
    public List<Test> getTests() {
        return List.of(
                new EvenTest(),
                new OddTest(),
                new ArrayTest(),
                new CollectionTest(),
                new MapTest(),
                new IterableTest(),
                new HasAllTest(),
                new HasAnyTest(),
                new HasNoneTest(),
                new NullTest(),
                new TypeTest(),
                new StartsTest(),
                new EndsTest(),
                new ContainsTest()
        );
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(
                // jmouse-el — the expression half, unchanged
                new ExpressionParser(),
                new OperatorParser(),
                new PrimaryExpressionParser(),
                new AutodetectFirstParser(),
                new RangeParser(),
                new FunctionParser(),
                new ScopedCallParser(),
                new BeanAccessParser(),
                // ⚠️ Registered so they PARSE, not so they may be used. `PrimaryExpressionParser` reads
                // `(a)` as the opening of a lambda before it reads it as a parenthesised expression, so
                // a language that left LambdaParser out would fail on ordinary brackets — and the three
                // shapes below are then refused, by name and with a line, when the binder audits the
                // tree. Refusing at bind says what is wrong; refusing by omission says "unexpected
                // token" about a bracket.
                new LambdaParser(),
                new PlaceholderParser(),
                new VariableAliasParser(),
                new TestParser(),
                new FilterParser(),
                new PropertyParser(),
                new LiteralParser(),
                new ArrayParser(),
                new MapParser(),
                new KeyValueParser(),
                new ParenthesesParser(),
                new ArgumentsParser(),
                new NamesParser(),
                new ParametersParser(),
                // jms specific — the order here is documented by ParserPriority, not by this list
                new StatementsParser(),
                new ScriptBodyParser(),
                new ScriptDocumentParser(),
                new ScriptParser(),
                new BehaviourParser(),
                new IncludeParser(),
                new HandlerParser(),
                new FunctionDeclarationParser(),
                new BranchParser(),
                new LoopParser(),
                new LocalParser(),
                new ReturnParser(),
                new AssignmentParser()
        );
    }

    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>();

        operators.addAll(Arrays.asList(MathematicOperator.values()));
        operators.addAll(Arrays.asList(UnaryOperator.values()));
        operators.addAll(Arrays.asList(LogicalOperator.values()));
        operators.addAll(Arrays.asList(ComparisonOperator.values()));

        operators.add(FilterOperator.FILTER);
        operators.add(TestOperator.IS);
        operators.add(InOperator.IN);
        operators.add(NullCoalesceOperator.NULL_COALESCE);
        operators.add(RangeOperator.RANGE);
        operators.add(ConcatOperator.CONCAT);

        return operators;
    }

    /**
     * The function names this dialect provides on its own, beside whatever a host declares.
     *
     * <p>⚠️ Read by the binder, so that {@code sqrt(x)} is not refused as "a function this host does not
     * declare" — the host did not declare it and never should have to. Derived from
     * {@link #getFunctions()} rather than written out again, because two lists of the same names is one
     * list and a bug waiting for somebody to add a function to only one of them.</p>
     *
     * @return the built-in function names
     */
    public static Set<String> builtinFunctions() {
        return FUNCTION_NAMES;
    }

    /**
     * The filter names this dialect provides.
     *
     * @return the built-in filter names
     */
    public static Set<String> builtinFilters() {
        return FILTER_NAMES;
    }

    /**
     * The test names this dialect provides.
     *
     * @return the built-in test names
     */
    public static Set<String> builtinTests() {
        return TEST_NAMES;
    }

    /**
     * Collects the names of a list of extension elements.
     *
     * @param elements the registered elements
     * @param name    how to ask one for its name
     * @param <E>     the element type
     * @return their names
     */
    private static <E> Set<String> namesOf(List<E> elements, java.util.function.Function<E, String> name) {
        return elements.stream().map(name).collect(Collectors.toUnmodifiableSet());
    }
}
