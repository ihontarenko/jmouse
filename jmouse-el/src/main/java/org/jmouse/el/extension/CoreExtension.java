package org.jmouse.el.extension;

import org.jmouse.core.bind.AttributeResolver;
import org.jmouse.el.extension.attribute.JavaBeanAttributeResolver;
import org.jmouse.el.extension.attribute.ListAttributeResolver;
import org.jmouse.el.extension.attribute.MapAttributeResolver;
import org.jmouse.el.extension.filter.*;
import org.jmouse.el.extension.filter.converter.*;
import org.jmouse.el.extension.function.*;
import org.jmouse.el.extension.function.string.*;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.*;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A standard implementation of the {@link Extension} interface.
 * <p>
 * This class aggregates functions, filters, tests, parsers, tag parsers, and operators
 * used in the expression language.
 * </p>
 */
public class CoreExtension implements Extension {

    /**
     * Returns custom attribute resolvers to handle XML/HTML‑style attributes.
     *
     * @return a list of {@link AttributeResolver} instances, or an empty list
     */
    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return List.of(
                new JavaBeanAttributeResolver(),
                new MapAttributeResolver(),
                new ListAttributeResolver()
        );
    }

    /**
     * Returns the list of functions available in the expression language.
     *
     * @return a list of {@link Function} instances
     */
    @Override
    public List<Function> getFunctions() {
        return List.of(
                new MinFunction(),
                new MaxFunction(),
                new SetFunction(),
                // string functions
                new UclastFunction(),
                new UcfirstFunction(),
                new LclastFunction(),
                new LcfirstFunction()
        );
    }

    /**
     * Returns the list of filters available in the expression language.
     *
     * @return a list of {@link Filter} instances
     */
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
                new TypeFilter(),
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
                new ToBigIntFilter(),
                new ToBigDecimalFilter(),
                new ToStringFilter(),
                new ToListFilter(),
                new ToArrayFilter(),
                new ToIteratorFilter(),
                // specific
                new SoutFilter()
        );
    }

    /**
     * Returns the list of tests available in the expression language.
     *
     * @return a list of {@link Test} instances
     */
    @Override
    public List<Test> getTests() {
        return List.of(
                new EvenTest(),
                new OddTest(),
                new ArrayTest(),
                new CollectionTest(),
                new MapTest(),
                new IterableTest(),
                new ContainsTest(),
                new NullTest(),
                new TypeTest(),
                new StartsTest(),
                new EndsTest()
        );
    }

    /**
     * Returns the list of parsers used for parser expressions.
     *
     * @return a list of {@link Parser} instances
     */
    @Override
    public List<Parser> getParsers() {
        return List.of(
                new ExpressionParser(),
                new OperatorParser(),
                new PrimaryExpressionParser(),
                new RangeParser(),
                new FunctionParser(),
                new LambdaParser(),
                new ScopedCallParser(),
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
                new ParametersParser()
        );
    }

    /**
     * Returns the list of operators available in the expression language.
     *
     * @return a list of {@link Operator} instances
     */
    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>();

        operators.addAll(Arrays.asList(MathematicOperator.values()));
        operators.addAll(Arrays.asList(UnaryOperator.values()));
        operators.addAll(Arrays.asList(LogicalOperator.values()));
        operators.addAll(Arrays.asList(ComparisonOperator.values()));

        operators.add(FilterOperator.FILTER);
        // operators.add(AttributeAccessOperator.ACCESS);
        operators.add(TestOperator.IS);
        operators.add(NullCoalesceOperator.NULL_COALESCE);
        operators.add(RangeOperator.RANGE);
        operators.add(ConcatOperator.CONCAT);

        return operators;
    }
}
