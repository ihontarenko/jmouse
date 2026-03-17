package org.jmouse.meterializer;

import java.util.Map;

/**
 * Structural and behavioral directive applied to a materialized node. 🧩
 *
 * <p>
 * {@code NodeDirective} represents a conditional operation executed during
 * template materialization. Directives allow templates to modify the produced
 * node tree without embedding imperative logic into template definitions.
 * </p>
 *
 * <h3>Typical use cases</h3>
 * <ul>
 *     <li>Conditional attribute manipulation</li>
 *     <li>Conditional node wrapping</li>
 *     <li>Conditional node omission</li>
 *     <li>Dynamic class manipulation</li>
 *     <li>Applying attribute collections</li>
 * </ul>
 *
 * <p>
 * Each directive is evaluated against the current {@link RenderingExecution}
 * context using a {@link TemplatePredicate}. If the predicate evaluates to
 * {@code true}, the directive is applied.
 * </p>
 */
public sealed interface NodeDirective
        permits NodeDirective.SetAttributeIf,
                NodeDirective.RemoveAttributeIf,
                NodeDirective.AddClassIf,
                NodeDirective.WrapIf,
                NodeDirective.OmitIf,
                NodeDirective.ApplyAttributes {

    /**
     * Conditionally sets or overrides an attribute. 🏷
     *
     * <p>If the predicate evaluates to {@code true}, the attribute is added
     * or replaced with the evaluated value.</p>
     *
     * @param predicate condition controlling application
     * @param name attribute name
     * @param value attribute value expression
     */
    record SetAttributeIf(
            TemplatePredicate predicate,
            String name,
            ValueExpression value
    ) implements NodeDirective { }

    /**
     * Conditionally removes an attribute. ❌
     *
     * <p>If the predicate evaluates to {@code true}, the specified attribute
     * is removed from the node.</p>
     *
     * @param predicate condition controlling removal
     * @param attributeName attribute to remove
     */
    record RemoveAttributeIf(
            TemplatePredicate predicate,
            String attributeName
    ) implements NodeDirective { }

    /**
     * Conditionally adds CSS class values. 🎨
     *
     * <p>
     * The provided expression is evaluated and appended to the node's
     * {@code class} attribute if the predicate is satisfied.
     * </p>
     *
     * @param predicate condition controlling application
     * @param classValue class name expression
     */
    record AddClassIf(
            TemplatePredicate predicate,
            ValueExpression classValue
    ) implements NodeDirective { }

    /**
     * Conditionally wraps a node with another element. 📦
     *
     * <p>
     * When the predicate evaluates to {@code true}, the current node is wrapped
     * inside a newly created element.
     * </p>
     *
     * <p>Example result:</p>
     *
     * <pre>{@code
     * before:
     *   <input>
     *
     * after:
     *   <div class="wrapper">
     *       <input>
     *   </div>
     * }</pre>
     *
     * @param predicate condition controlling wrapping
     * @param wrapperTagName wrapper element tag
     * @param wrapperAttributes attributes applied to wrapper
     */
    record WrapIf(
            TemplatePredicate predicate,
            String wrapperTagName,
            Map<String, ValueExpression> wrapperAttributes
    ) implements NodeDirective { }

    /**
     * Conditionally omits a node from the result tree. 🚫
     *
     * <p>
     * If the predicate evaluates to {@code true}, the node is not included
     * in the materialized output.
     * </p>
     *
     * @param predicate condition controlling omission
     */
    record OmitIf(
            TemplatePredicate predicate
    ) implements NodeDirective { }

    /**
     * Applies attributes from a structured source. 🔗
     *
     * <p>
     * The {@code source} expression must evaluate to a collection of attribute
     * descriptors (for example a map or list of key/value entries). Each entry
     * is mapped to node attributes using the provided key names.
     * </p>
     *
     * <p>This directive is commonly used to apply dynamic attribute sets:</p>
     *
     * <pre>{@code
     * attributes(path("attributes"), "name", "value")
     * }</pre>
     *
     * @param source attribute collection expression
     * @param keyValue property containing attribute name
     * @param valueKey property containing attribute value
     */
    record ApplyAttributes(
            ValueExpression source,
            String keyValue,
            String valueKey
    ) implements NodeDirective { }

}