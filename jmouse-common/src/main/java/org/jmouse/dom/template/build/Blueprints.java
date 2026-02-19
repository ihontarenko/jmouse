package org.jmouse.dom.template.build;

import org.jmouse.dom.template.*;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Fluent factory for building blueprint trees.
 */
public final class Blueprints {

    private Blueprints() {}

    public static ValueExpression constant(Object value) {
        return new ValueExpression.ConstantValue(value);
    }

    public static ValueExpression path(String path) {
        return new ValueExpression.PathValue(path);
    }

    public static ValueExpression format(String pattern, ValueExpression... arguments) {
        return new ValueExpression.FormatValue(pattern, List.of(arguments));
    }

    public static TemplatePredicate pathBoolean(String path) {
        return new TemplatePredicate.BooleanValue(
                path(path)
        );
    }

    public static NodeTemplate.Text text(ValueExpression value) {
        return new NodeTemplate.Text(value);
    }

    public static NodeTemplate.Text text(String constantText) {
        return new NodeTemplate.Text(constant(constantText));
    }

    public static ValueExpression requestAttribute(String name) {
        return new ValueExpression.RequestAttributeValue(name);
    }

    public static NodeTemplate.Element element(String tagName, Consumer<ElementBlueprintBuilder> consumer) {
        ElementBlueprintBuilder builder = new ElementBlueprintBuilder(tagName);
        consumer.accept(builder);
        return builder.build();
    }

    public static NodeTemplate.Conditional when(
            TemplatePredicate predicate,
            Consumer<BlueprintListBuilder> whenTrue,
            Consumer<BlueprintListBuilder> whenFalse
    ) {
        BlueprintListBuilder trueBuilder = new BlueprintListBuilder();
        BlueprintListBuilder falseBuilder = new BlueprintListBuilder();

        whenTrue.accept(trueBuilder);
        whenFalse.accept(falseBuilder);

        return new NodeTemplate.Conditional(predicate, trueBuilder.build(), falseBuilder.build());
    }

    public static NodeTemplate.Conditional when(
            TemplatePredicate predicate,
            Consumer<BlueprintListBuilder> whenTrue
    ) {
        BlueprintListBuilder trueBuilder = new BlueprintListBuilder();
        whenTrue.accept(trueBuilder);

        return new NodeTemplate.Conditional(predicate, trueBuilder.build(), List.of());
    }

    public static NodeTemplate.Repeat repeat(
            ValueExpression collection,
            String itemVariableName,
            Consumer<BlueprintListBuilder> body
    ) {
        BlueprintListBuilder builder = new BlueprintListBuilder();
        body.accept(builder);
        return new NodeTemplate.Repeat(collection, itemVariableName, builder.build());
    }

    public static List<NodeTemplate> list(Consumer<BlueprintListBuilder> consumer) {
        BlueprintListBuilder builder = new BlueprintListBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    public static Map<String, ValueExpression> attributes(Consumer<AttributeMapBuilder> consumer) {
        AttributeMapBuilder builder = new AttributeMapBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    public static TemplatePredicate truthy(ValueExpression value) {
        return new TemplatePredicate.BooleanValue(value);
    }

    public static TemplatePredicate present(ValueExpression value) {
        return new TemplatePredicate.Present(value);
    }

    public static TemplatePredicate same(ValueExpression left, ValueExpression right) {
        return new TemplatePredicate.Equality(left, right);
    }

    public static TemplatePredicate not(TemplatePredicate inner) {
        return new TemplatePredicate.Not(inner);
    }

    public static TemplatePredicate all(TemplatePredicate... predicates) {
        return new TemplatePredicate.All(List.of(predicates));
    }

    public static TemplatePredicate any(TemplatePredicate... predicates) {
        return new TemplatePredicate.Any(List.of(predicates));
    }

    public static TemplatePredicate contains(ValueExpression collection, ValueExpression value) {
        return new TemplatePredicate.Contains(collection, value);
    }

}
