package org.jmouse.dom.meterializer;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;

import java.util.List;
import java.util.Map;

public class DOMMaterializer extends AbstractTemplateMaterializer<Node> {

    public DOMMaterializer() {
        super(new DefaultValueResolver(new PathValueResolver()));
    }

    public DOMMaterializer(ValueResolver valueResolver) {
        super(Verify.nonNull(valueResolver, "valueResolver"));
    }

    @Override
    protected Node createElementNode(String tagName) {
        Verify.nonNull(tagName, "tagName");
        return new ElementNode(toTagName(tagName));
    }

    @Override
    protected Node createTextNode(String text) {
        return new TextNode(text == null ? "" : text);
    }

    @Override
    protected Node createContainerNode() {
        return new ElementNode(TagName.DIV);
    }

    @Override
    protected Node emptyNode() {
        return new TextNode("");
    }

    @Override
    protected void appendChild(Node parent, Node child) {
        Verify.nonNull(parent, "parent");
        Verify.nonNull(child, "child");

        if (!(parent instanceof ElementNode element)) {
            throw new IllegalStateException("Cannot append child to non-element node: " + parent.getClass().getName());
        }

        element.append(child);
    }

    @Override
    protected Node materializeElement(NodeTemplate.Element element, RenderingExecution execution) {
        Verify.nonNull(element, "element");
        Verify.nonNull(execution, "execution");

        ElementNode node = new ElementNode(toTagName(element.tagName()));

        applyAttributes(node, element.attributes(), execution);

        DirectiveOutcome<Node> outcome = applyDirectives(node, element.directives(), execution);

        if (outcome.isOmitted()) {
            return null;
        }

        ElementNode contentNode = (ElementNode) outcome.node();

        for (NodeTemplate child : element.children()) {
            Node childNode = materializeInternal(child, execution);
            if (childNode != null) {
                contentNode.append(childNode);
            }
        }

        return outcome.root();
    }

    private void applyAttributes(
            ElementNode node,
            Map<String, ValueExpression> attributes,
            RenderingExecution execution
    ) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ValueExpression> entry : attributes.entrySet()) {
            String name = entry.getKey();
            ValueExpression expression = entry.getValue();

            if (name == null || name.isBlank()) {
                continue;
            }
            if (expression == null) {
                continue;
            }

            Object value = valueResolver.resolve(expression, execution);

            if (value == null) {
                continue;
            }

            node.addAttribute(name, String.valueOf(value));
        }
    }

    private DirectiveOutcome<Node> applyDirectives(
            ElementNode node,
            List<NodeDirective> directives,
            RenderingExecution execution
    ) {
        if (directives == null || directives.isEmpty()) {
            return DirectiveOutcome.keep(node);
        }

        Node root = node;

        for (NodeDirective directive : directives) {
            switch (directive) {
                case NodeDirective.OmitIf omitIf -> {
                    if (predicateEvaluator.evaluate(omitIf.predicate(), execution)) {
                        return DirectiveOutcome.omit();
                    }
                }
                case NodeDirective.SetAttributeIf attributeIf -> {
                    if (predicateEvaluator.evaluate(attributeIf.predicate(), execution)) {
                        Object value = valueResolver.resolve(attributeIf.value(), execution);
                        if (value != null) {
                            node.addAttribute(attributeIf.name(), String.valueOf(value));
                        }
                    }
                }
                case NodeDirective.RemoveAttributeIf removeIf -> {
                    if (predicateEvaluator.evaluate(removeIf.predicate(), execution)) {
                        node.getAttributes().remove(removeIf.attributeName());
                    }
                }
                case NodeDirective.AddClassIf classIf -> {
                    if (predicateEvaluator.evaluate(classIf.predicate(), execution)) {
                        Object value = valueResolver.resolve(classIf.classValue(), execution);
                        if (value != null) {
                            String classNames = String.valueOf(value).trim();
                            if (!classNames.isEmpty()) {
                                Node.addClass(node, classNames);
                            }
                        }
                    }
                }
                case NodeDirective.WrapIf wrapIf -> {
                    if (predicateEvaluator.evaluate(wrapIf.predicate(), execution)) {
                        ElementNode wrapper = new ElementNode(toTagName(wrapIf.wrapperTagName()));
                        applyAttributes(wrapper, wrapIf.wrapperAttributes(), execution);
                        node.wrap(wrapper);
                        root = wrapper;
                    }
                }
                case null -> {
                    // ignore
                }
            }
        }

        return DirectiveOutcome.wrapped(node, root);
    }

    private TagName toTagName(String tagName) {
        String normalized = Verify.nonNull(tagName, "tagName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("tagName is blank");
        }
        return TagName.valueOf(normalized.toUpperCase());
    }

}
