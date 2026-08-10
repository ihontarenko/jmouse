package org.jmouse.access.el;

import org.jmouse.access.el.parser.*;
import org.jmouse.core.access.AttributeResolver;
import org.jmouse.el.extension.*;
import org.jmouse.el.extension.attribute.JavaBeanAttributeResolver;
import org.jmouse.el.extension.attribute.ListAttributeResolver;
import org.jmouse.el.extension.attribute.MapAttributeResolver;
import org.jmouse.el.extension.filter.*;
import org.jmouse.el.extension.filter.converter.*;
import org.jmouse.el.extension.filter.datetime.MinusDaysFilter;
import org.jmouse.el.extension.filter.datetime.PlusDaysFilter;
import org.jmouse.el.extension.function.*;
import org.jmouse.el.extension.function.datetime.NowFunction;
import org.jmouse.el.extension.function.mathematic.ExponentaFunction;
import org.jmouse.el.extension.function.mathematic.SquareRootFunction;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.*;
import org.jmouse.el.language.parser.IfParser;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Everything the {@code .jmp} language is made of, registered as one extension.
 *
 * <p>It is the expression language plus the policy language, not the policy language alone: a file
 * needs literals, property access and operators to write a {@code when} clause with, and the
 * placeholder parser to read a {@code ${…}} with. What a condition may then <em>use</em> is a
 * narrower question, answered somewhere else entirely — see
 * {@link org.jmouse.access.el.condition.ConditionDialect}, which builds its own container precisely
 * so that a policy file's parser and a policy condition's evaluator never share a vocabulary.</p>
 *
 * <p>⚠️ The order of {@link #getParsers()} carries no meaning. Dispatch sorts by
 * {@link org.jmouse.access.el.parser.ParserPriority}, and that class — not this list — is where the
 * reason each shape is offered before another is written down.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ExpressionExtension implements Extension {

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
                new SetVariableFunction(),
                new GetVariableFunction(),
                new SquareRootFunction(),
                new ExponentaFunction(),
                new NowFunction()
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
                new ToBigIntFilter(),
                new ToBigDecimalFilter(),
                new ToStringFilter(),
                new ToListFilter(),
                new ToArrayFilter(),
                new ToIteratorFilter(),
                new ToInstantFilter(),
                // date-time
                new PlusDaysFilter(),
                new MinusDaysFilter()
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
                new ExpressionParser(),
                new OperatorParser(),
                new PrimaryExpressionParser(),
                new AutodetectFirstParser(),
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
                new ParametersParser(),
                new VariableAliasParser(),
                new PlaceholderParser(),
                // jme-language
                new IfParser(),
                // jmp specific — the order here is documented by ParserPriority, not by this list
                new StatementsParser(),
                new PolicyDocumentParser(),
                new PolicyParser(),
                new ScopesParser(),
                new ScopeDeclarationParser(),
                new PermissionsParser(),
                new PermissionDeclarationParser(),
                new PermissionValueParser(),
                new RoleParser(),
                new SubjectParser(),
                new RoleAssignmentParser(),
                new GrantParser(),
                new SingleScopeParser(),
                new IncludeParser()
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
        // operators.add(AttributeAccessOperator.ACCESS);
        operators.add(TestOperator.IS);
        operators.add(InOperator.IN);
        operators.add(NullCoalesceOperator.NULL_COALESCE);
        operators.add(RangeOperator.RANGE);
        operators.add(ConcatOperator.CONCAT);

        return operators;
    }
}
