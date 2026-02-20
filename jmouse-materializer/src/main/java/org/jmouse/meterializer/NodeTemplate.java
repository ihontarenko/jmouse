package org.jmouse.meterializer;

import org.jmouse.meterializer.build.ElementBlueprintBuilder;
import org.jmouse.meterializer.build.TemplateNodeCollectionBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Intermediate immutable blueprint representing a markup tree. 🏗️
 *
 * <p>
 * A {@code NodeTemplate} describes structure and behavior of a template
 * before it is transformed and materialized. It is a declarative model —
 * no actual rendering occurs at this stage.
 * </p>
 *
 * <p>
 * Typical rendering pipeline:
 * </p>
 *
 * <pre>{@code
 * blueprint → transform(...) → materialize(...)
 * }</pre>
 *
 * <h3>Supported node types</h3>
 * <ul>
 *     <li>{@link Element} — structural element node</li>
 *     <li>{@link Text} — text content node</li>
 *     <li>{@link Conditional} — conditional branching</li>
 *     <li>{@link Repeat} — iteration over a collection</li>
 *     <li>{@link Include} — nested blueprint inclusion</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * NodeTemplate template = NodeTemplate.element("div", div -> div
 *     .attribute("class", ValueExpression.constant("card"))
 *     .children(children -> children
 *         .text("Hello ")
 *         .text(ValueExpression.path("user.name"))
 *     )
 * );
 * }</pre>
 *
 * <p>
 * All implementations are immutable records.
 * Evaluation semantics are defined by {@link TemplateMaterializer}.
 * </p>
 */
public sealed interface NodeTemplate
        permits NodeTemplate.Element,
        NodeTemplate.Text,
        NodeTemplate.Conditional,
        NodeTemplate.Repeat,
        NodeTemplate.Include {

    /**
     * Includes another blueprint by a constant key.
     *
     * <p>
     * Shortcut for {@link #include(ValueExpression, ValueExpression)} where the key is constant.
     * </p>
     *
     * @param keyConstant blueprint key (constant)
     * @param model model expression (object to pass as new root; can be path-bound)
     * @return include node
     */
    static NodeTemplate include(String keyConstant, ValueExpression model) {
        return new NodeTemplate.Include(ValueExpression.constant(keyConstant), model);
    }

    /**
     * Includes another blueprint by key.
     *
     * <p>
     * The {@code keyExpression} is resolved at runtime (constant or path-bound),
     * allowing dynamic template selection.
     * </p>
     *
     * @param keyExpression blueprint key expression
     * @param model model expression (object to pass as new root; can be path-bound)
     * @return include node
     */
    static NodeTemplate include(ValueExpression keyExpression, ValueExpression model) {
        return new NodeTemplate.Include(keyExpression, model);
    }

    /**
     * Builds an immutable list of nodes using {@link TemplateNodeCollectionBuilder}. 🧱
     *
     * <pre>{@code
     * List<NodeTemplate> nodes = NodeTemplate.list(b -> b
     *     .text("Hello")
     *     .element("span", e -> e
     *         .text(ValueExpression.path("user.name"))
     *     )
     * );
     * }</pre>
     *
     * @param consumer builder customization callback
     * @return immutable list of nodes
     */
    static List<NodeTemplate> list(Consumer<TemplateNodeCollectionBuilder> consumer) {
        TemplateNodeCollectionBuilder builder = new TemplateNodeCollectionBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    /**
     * Creates an {@link Element} node using {@link ElementBlueprintBuilder}.
     *
     * @param qName element name
     * @param consumer builder customization callback
     * @return element node
     */
    static NodeTemplate.Element element(QName qName, Consumer<ElementBlueprintBuilder> consumer) {
        ElementBlueprintBuilder builder = new ElementBlueprintBuilder(qName);
        consumer.accept(builder);
        return builder.build();
    }

    /**
     * Creates an {@link Element} node with a local tag name (no namespace). 🏷️
     *
     * @param tagName local tag name (e.g. {@code "div"})
     * @param consumer builder customization callback
     * @return element node
     */
    static NodeTemplate.Element element(String tagName, Consumer<ElementBlueprintBuilder> consumer) {
        return element(QName.local(tagName), consumer);
    }

    /**
     * Creates a conditional node with both branches.
     *
     * <pre>{@code
     * NodeTemplate node = NodeTemplate.when(
     *     TemplatePredicate.present(ValueExpression.path("user.name")),
     *     t -> t.text("Hello "),
     *     f -> f.text("Anonymous")
     * );
     * }</pre>
     *
     * @param predicate condition deciding which branch to materialize
     * @param branchA nodes materialized when predicate is {@code true}
     * @param branchB nodes materialized when predicate is {@code false}
     * @return conditional node
     */
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

    /**
     * Creates a conditional node with a single {@code true} branch.
     *
     * <p>
     * The {@code false} branch is represented as an empty list.
     * </p>
     *
     * @param predicate condition deciding whether branch is materialized
     * @param branchA nodes materialized when predicate is {@code true}
     * @return conditional node
     */
    static NodeTemplate.Conditional when(
            TemplatePredicate predicate,
            Consumer<TemplateNodeCollectionBuilder> branchA
    ) {
        TemplateNodeCollectionBuilder builder = new TemplateNodeCollectionBuilder();
        branchA.accept(builder);
        return new NodeTemplate.Conditional(predicate, builder.build(), List.of());
    }

    /**
     * Creates a repeat node with default wrapper tag {@code "div"}. 🔁
     *
     * <p>
     * Shortcut for {@link #repeat(ValueExpression, String, Consumer, String)}.
     * </p>
     *
     * @param collection expression resolving to a collection-like object
     * @param itemVariableName variable name used inside the body
     * @param body body builder callback
     * @return repeat node
     */
    static NodeTemplate.Repeat repeat(
            ValueExpression collection,
            String itemVariableName,
            Consumer<TemplateNodeCollectionBuilder> body
    ) {
        return repeat(collection, itemVariableName, body, "div");
    }

    /**
     * Creates a repeat node. 🔁
     *
     * <p>
     * During materialization, the {@code collection} is resolved, then the {@code body}
     * is materialized for each item with {@code itemVariableName} bound in execution scope.
     * </p>
     *
     * @param collection expression resolving to a collection-like object
     * @param itemVariableName variable name used inside the body
     * @param body body builder callback
     * @param tagName wrapper tag name used by the materializer
     * @return repeat node
     */
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

    /**
     * Creates a text node with the given expression.
     *
     * @param value text expression (constant or resolved)
     * @return text node
     */
    static NodeTemplate.Text text(ValueExpression value) {
        return new NodeTemplate.Text(value);
    }

    /**
     * Creates a text node with constant text.
     *
     * @param constantText constant text
     * @return text node
     */
    static NodeTemplate.Text text(String constantText) {
        return new NodeTemplate.Text(ValueExpression.constant(constantText));
    }

    /**
     * Represents an element node in a blueprint tree.
     *
     * <p>
     * {@code attributes} values are {@link ValueExpression}s and may be constant
     * or dynamically resolved (path/request/format).
     * </p>
     *
     * <p>
     * {@code directives} are optional node-level instructions that can influence
     * transformation/materialization (e.g. metadata, behaviors).
     * </p>
     *
     * @param qName element tag name (for example "div", "input", "select")
     * @param attributes element attributes; values can be constant or bound to a data path
     * @param children child blueprint nodes
     * @param directives optional directives associated with this node
     */
    record Element(
            QName qName,
            Map<String, ValueExpression> attributes,
            List<NodeTemplate> children,
            List<NodeDirective> directives
    ) implements NodeTemplate {

        /**
         * Returns the local tag name of this element.
         *
         * @return local tag name
         */
        public String tagName() {
            return qName.name();
        }

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
     * <p>
     * The {@code collection} is resolved at runtime, then {@code body} is materialized
     * for each item. The {@code itemVariableName} is expected to be bound into
     * execution scope for each iteration.
     * </p>
     *
     * @param collection value that resolves to a collection-like object
     * @param itemVariableName variable name used inside the body
     * @param body blueprint nodes to be materialized for each element
     * @param tagName wrapper tag name used by the materializer
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
     * <p>
     * {@code blueprintKey} may be constant or dynamically resolved.
     * The {@code model} expression defines a new root object for the included scope.
     * </p>
     *
     * @param blueprintKey key expression (can be constant or path-bound)
     * @param model model expression (object to pass as new root; can be path-bound)
     */
    record Include(
            ValueExpression blueprintKey,
            ValueExpression model
    ) implements NodeTemplate {}

}