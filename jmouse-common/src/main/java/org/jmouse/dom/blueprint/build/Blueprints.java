package org.jmouse.dom.blueprint.build;

import org.jmouse.dom.blueprint.*;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Fluent factory for building blueprint trees.
 */
public final class Blueprints {

    private Blueprints() {}

    public static BlueprintValue constant(Object value) {
        return new BlueprintValue.ConstantValue(value);
    }

    public static BlueprintValue path(String path) {
        return new BlueprintValue.PathValue(path);
    }

    public static BlueprintPredicate pathBoolean(String path) {
        return new BlueprintPredicate.PathBooleanPredicate(path);
    }

    public static Blueprint.TextBlueprint text(BlueprintValue value) {
        return new Blueprint.TextBlueprint(value);
    }

    public static Blueprint.TextBlueprint text(String constantText) {
        return new Blueprint.TextBlueprint(constant(constantText));
    }

    public static BlueprintValue requestAttribute(String name) {
        return new BlueprintValue.RequestAttributeValue(name);
    }

    public static Blueprint.ElementBlueprint element(String tagName, Consumer<ElementBlueprintBuilder> consumer) {
        ElementBlueprintBuilder builder = new ElementBlueprintBuilder(tagName);
        consumer.accept(builder);
        return builder.build();
    }

    public static Blueprint.ConditionalBlueprint when(
            BlueprintPredicate predicate,
            Consumer<BlueprintListBuilder> whenTrue,
            Consumer<BlueprintListBuilder> whenFalse
    ) {
        BlueprintListBuilder trueBuilder = new BlueprintListBuilder();
        BlueprintListBuilder falseBuilder = new BlueprintListBuilder();

        whenTrue.accept(trueBuilder);
        whenFalse.accept(falseBuilder);

        return new Blueprint.ConditionalBlueprint(predicate, trueBuilder.build(), falseBuilder.build());
    }

    public static Blueprint.ConditionalBlueprint when(
            BlueprintPredicate predicate,
            Consumer<BlueprintListBuilder> whenTrue
    ) {

        BlueprintListBuilder trueBuilder = new BlueprintListBuilder();
        whenTrue.accept(trueBuilder);

        return new Blueprint.ConditionalBlueprint(predicate, trueBuilder.build(), List.of());
    }

    public static Blueprint.RepeatBlueprint repeat(BlueprintValue collection,
                                                   String itemVariableName,
                                                   Consumer<BlueprintListBuilder> body) {

        BlueprintListBuilder bodyBuilder = new BlueprintListBuilder();
        body.accept(bodyBuilder);

        return new Blueprint.RepeatBlueprint(collection, itemVariableName, bodyBuilder.build());
    }

    public static List<Blueprint> list(Consumer<BlueprintListBuilder> consumer) {
        BlueprintListBuilder builder = new BlueprintListBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    public static Map<String, BlueprintValue> attributes(Consumer<AttributeMapBuilder> consumer) {
        AttributeMapBuilder builder = new AttributeMapBuilder();
        consumer.accept(builder);
        return builder.build();
    }
}
