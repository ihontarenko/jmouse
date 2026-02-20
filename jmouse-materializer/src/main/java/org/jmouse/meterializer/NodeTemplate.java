package org.jmouse.meterializer;

import org.jmouse.meterializer.build.ElementBlueprintBuilder;
import org.jmouse.meterializer.build.TemplateNodeCollectionBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Intermediate representation of a markup tree that can be transformed before materialization.
 */
public sealed interface NodeTemplate
        permits NodeTemplate.Element,
                NodeTemplate.Text,
                NodeTemplate.Conditional,
                NodeTemplate.Repeat,
                NodeTemplate.Include {

    public static List<NodeTemplate> list(Consumer<TemplateNodeCollectionBuilder> consumer) {
        TemplateNodeCollectionBuilder builder = new TemplateNodeCollectionBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    static NodeTemplate.Element element(String tagName, Consumer<ElementBlueprintBuilder> consumer) {
        ElementBlueprintBuilder builder = new ElementBlueprintBuilder(tagName);
        consumer.accept(builder);
        return builder.build();
    }

    static NodeTemplate.Conditional when(
            TemplatePredicate predicate,
            Consumer<TemplateNodeCollectionBuilder> branchA,
            Consumer<TemplateNodeCollectionBuilder> branchB
    ) {
        TemplateNodeCollectionBuilder builderA  = new TemplateNodeCollectionBuilder();
        TemplateNodeCollectionBuilder builderB = new TemplateNodeCollectionBuilder();

        branchA.accept(builderA);
        branchB.accept(builderB);

        return new NodeTemplate.Conditional(predicate, builderA.build(), builderB.build());
    }

    static NodeTemplate.Conditional when(
            TemplatePredicate predicate,
            Consumer<TemplateNodeCollectionBuilder> branchA
    ) {
        TemplateNodeCollectionBuilder builder = new TemplateNodeCollectionBuilder();
        branchA.accept(builder);
        return new NodeTemplate.Conditional(predicate, builder.build(), List.of());
    }

    static NodeTemplate.Repeat repeat(
            ValueExpression collection,
            String itemVariableName,
            Consumer<TemplateNodeCollectionBuilder> body
    ) {
        return repeat(collection, itemVariableName, body, "div");
    }

    static NodeTemplate.Repeat repeat(
            ValueExpression collection,
            String itemVariableName,
            Consumer<TemplateNodeCollectionBuilder> body,
            String tagName
    ) {
        TemplateNodeCollectionBuilder builder = new TemplateNodeCollectionBuilder();
        body.accept(builder);
        return new NodeTemplate.Repeat(collection, itemVariableName, builder.build(), tagName);
    }

    static NodeTemplate.Text text(ValueExpression value) {
        return new NodeTemplate.Text(value);
    }

    static NodeTemplate.Text text(String constantText) {
        return new NodeTemplate.Text(ValueExpression.constant(constantText));
    }

    /**
     * Represents an element node in a blueprint tree.
     *
     * @param tagName element tag name (for example "div", "input", "select")
     * @param attributes element attributes; values can be constant or bound to a data path
     * @param children child blueprint nodes
     */
    record Element(
            String tagName,
            Map<String, ValueExpression> attributes,
            List<NodeTemplate> children,
            List<NodeDirective> directives
    ) implements NodeTemplate {
    }

    /**
     * Represents a text node in a blueprint tree.
     *
     * @param value constant or bound value
     */
    record Text(ValueExpression value) implements NodeTemplate {
    }

    /**
     * Represents a conditional branch in a blueprint tree.
     *
     * @param predicate predicate that decides which branch to materialize
     * @param whenTrue nodes materialized when predicate is true
     * @param whenFalse nodes materialized when predicate is false
     */
    record Conditional(
            TemplatePredicate predicate,
            List<NodeTemplate> whenTrue,
            List<NodeTemplate> whenFalse
    ) implements NodeTemplate {
    }

    /**
     * Represents a repeatable section in a blueprint tree.
     *
     * @param collection value that resolves to a collection-like object
     * @param itemVariableName variable name used inside the body
     * @param body blueprint nodes to be materialized for each element
     */
    record Repeat(
            ValueExpression collection,
            String itemVariableName,
            List<NodeTemplate> body,
            String tagName
    ) implements NodeTemplate {
    }

    /**
     * Includes another blueprint by key.
     *
     * @param blueprintKey key expression (can be constant or path-bound)
     * @param model model expression (object to pass as new root; can be path-bound)
     */
    record Include(
            ValueExpression blueprintKey,
            ValueExpression model
    ) implements NodeTemplate {}

}
